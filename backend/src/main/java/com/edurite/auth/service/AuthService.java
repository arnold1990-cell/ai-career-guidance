package com.edurite.auth.service;

import com.edurite.auth.config.EmailVerificationProperties;
import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.CompanyRegisterRequest;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.dto.RegistrationResponse;
import com.edurite.auth.dto.StudentRegisterRequest;
import com.edurite.auth.dto.VerificationEmailRequest;
import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.auth.entity.EmailVerificationToken;
import com.edurite.auth.exception.EmailVerificationRequiredException;
import com.edurite.auth.exception.InvalidVerificationTokenException;
import com.edurite.auth.repository.EmailVerificationTokenRepository;
import com.edurite.common.exception.DuplicateEmailException;
import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.email.service.EmailService;
import com.edurite.security.service.JwtService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final List<String> ROLE_PRIORITY = List.of("ROLE_ADMIN", "ROLE_COMPANY", "ROLE_STUDENT");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final EmailVerificationProperties emailVerificationProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            CompanyProfileRepository companyProfileRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            EmailService emailService,
            EmailVerificationProperties emailVerificationProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.emailVerificationProperties = emailVerificationProperties;
    }

    @Transactional
    public RegistrationResponse registerStudent(StudentRegisterRequest request) {
        String[] names = splitFullName(request.fullName());
        String firstName = trimToNull(request.firstName());
        String lastName = trimToNull(request.lastName());
        String resolvedFirstName = firstName != null ? firstName : names[0];
        String resolvedLastName = lastName != null ? lastName : names[1];
        User user = createUser(
                request.email(),
                request.password(),
                resolvedFirstName,
                resolvedLastName,
                "ROLE_STUDENT",
                false
        );

        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        profile.setFirstName(user.getFirstName());
        profile.setLastName(user.getLastName());
        profile.setInterests(trimToNull(request.interests()));
        profile.setLocation(trimToNull(request.location()));
        profile.setPhone(trimToNull(request.phone()));
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setGender(trimToNull(request.gender()));
        profile.setQualificationLevel(trimToNull(request.qualificationLevel()));
        profile.setProfileCompleted(false);
        studentProfileRepository.save(profile);

        issueVerification(user);
        return new RegistrationResponse("Account created. Please check your email to verify your account.", user.getEmail(), true);
    }

    @Transactional
    public RegistrationResponse registerCompany(CompanyRegisterRequest request) {
        String officialEmail = request.officialEmail();
        if (officialEmail == null || officialEmail.isBlank()) {
            officialEmail = request.email();
        }
        officialEmail = normalizeEmail(officialEmail);

        if (officialEmail == null || officialEmail.isBlank()) {
            throw new ResourceConflictException("Company email is required");
        }

        String companyName = request.companyName() == null ? "" : request.companyName().trim();
        if (companyName.isBlank()) {
            throw new ResourceConflictException("Company name is required");
        }

        String contactPersonName = request.contactPersonName();
        if (contactPersonName == null || contactPersonName.isBlank()) {
            contactPersonName = companyName;
        } else {
            contactPersonName = contactPersonName.trim();
        }

        String registrationNumber = request.registrationNumber();
        if (registrationNumber == null || registrationNumber.isBlank()) {
            registrationNumber = "PENDING-" + UUID.randomUUID();
        } else {
            registrationNumber = registrationNumber.trim();
        }

        if (companyProfileRepository.existsByRegistrationNumberIgnoreCase(registrationNumber)) {
            throw new ResourceConflictException("Company registration number already exists");
        }

        User user = createUser(
                officialEmail,
                request.password(),
                contactPersonName,
                companyName,
                "ROLE_COMPANY",
                false
        );

        CompanyProfile profile = new CompanyProfile();
        profile.setUserId(user.getId());
        profile.setCompanyName(companyName);
        profile.setRegistrationNumber(registrationNumber);
        profile.setIndustry(request.industry());
        profile.setOfficialEmail(officialEmail);
        profile.setMobileNumber(request.mobileNumber());
        profile.setContactPersonName(contactPersonName);
        profile.setAddress(request.address());
        profile.setWebsite(request.website());
        profile.setDescription(request.description());
        profile.setStatus(CompanyApprovalStatus.PENDING);
        companyProfileRepository.save(profile);

        issueVerification(user);
        return new RegistrationResponse("Account created. Please check your email to verify your account.", user.getEmail(), true);
    }

    public AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidCredentialsException();
        }

        String username;
        try {
            if (!jwtService.isRefreshToken(refreshToken)) {
                throw new InvalidCredentialsException();
            }
            username = jwtService.extractUsername(refreshToken);
        } catch (RuntimeException ex) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(username).orElseThrow(InvalidCredentialsException::new);
        Set<String> effectiveRoles = resolveEffectiveRoles(user);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .authorities(effectiveRoles.toArray(String[]::new))
                .build();

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new InvalidCredentialsException();
        }

        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        Optional<User> candidateUser = userRepository.findByEmail(normalizedEmail);
        boolean passwordMatches = candidateUser
                .map(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElse(false);
        log.info("[auth] login requested email={} userFound={} passwordMatches={}", normalizedEmail, candidateUser.isPresent(), passwordMatches);
        if (candidateUser.isPresent() && passwordMatches && !candidateUser.get().isEmailVerified()) {
            log.warn("[auth] blocked login for unverified email={}", normalizedEmail);
            throw new EmailVerificationRequiredException();
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(InvalidCredentialsException::new);
            return toAuthResponse(user);
        } catch (BadCredentialsException ex) {
            log.warn("[auth] failed login attempt username={} reason=bad_credentials", normalizedEmail, ex);
            throw new InvalidCredentialsException();
        } catch (DisabledException ex) {
            log.warn("[auth] failed login attempt username={} reason=disabled", normalizedEmail, ex);
            throw new InvalidCredentialsException();
        }
    }

    @Transactional
    public VerificationStatusResponse verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("[auth] invalid verification token supplied");
                    return new InvalidVerificationTokenException("Verification link is invalid.");
                });

        if (verificationToken.isUsed()) {
            log.warn("[auth] attempted reuse of verification token id={}", verificationToken.getId());
            throw new InvalidVerificationTokenException("Verification link has already been used.");
        }

        if (verificationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.warn("[auth] expired verification token id={} userId={}", verificationToken.getId(), verificationToken.getUser().getId());
            throw new InvalidVerificationTokenException("Verification link has expired.");
        }

        User user = verificationToken.getUser();
        if (user.isEmailVerified()) {
            verificationToken.setUsed(true);
            verificationToken.setUsedAt(OffsetDateTime.now());
            emailVerificationTokenRepository.save(verificationToken);
            log.info("[auth] verification requested for already verified userId={}", user.getId());
            return new VerificationStatusResponse("Your email is already verified. You can sign in.");
        }

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        verificationToken.setUsed(true);
        verificationToken.setUsedAt(OffsetDateTime.now());
        userRepository.save(user);
        emailVerificationTokenRepository.save(verificationToken);
        log.info("[auth] verified email for userId={} email={}", user.getId(), user.getEmail());
        return new VerificationStatusResponse("Email verified successfully. You can now sign in.");
    }

    @Transactional
    public VerificationStatusResponse resendVerification(VerificationEmailRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (user.isEmailVerified()) {
                log.info("[auth] resend verification ignored for already verified email={}", normalizedEmail);
                return;
            }
            issueVerification(user);
        });
        return new VerificationStatusResponse("If an unverified account exists for that email, a new verification email has been sent.");
    }

    private User createUser(String email, String password, String firstName, String lastName, String roleName, boolean emailVerified) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceConflictException("Role '%s' does not exist".formatted(roleName)));

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(emailVerified);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private void issueVerification(User user) {
        emailVerificationTokenRepository.deleteByUserId(user.getId());
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(generateSecureToken());
        token.setExpiresAt(OffsetDateTime.now().plusHours(emailVerificationProperties.tokenValidityHours()));
        emailVerificationTokenRepository.save(token);
        emailService.sendEmailVerification(
                user.getEmail(),
                user.getFirstName(),
                buildVerificationUrl(token.getToken()),
                emailVerificationProperties.tokenValidityHours()
        );
    }

    private String buildVerificationUrl(String token) {
        String baseUrl = emailVerificationProperties.frontendUrlBase();
        String delimiter = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + delimiter + "token=" + token;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AuthResponse toAuthResponse(User user) {
        Set<String> roles = resolveEffectiveRoles(user);
        CompanyProfile companyProfile = companyProfileRepository.findByUserId(user.getId()).orElse(null);
        String approvalStatus = companyProfile == null ? null : companyProfile.getStatus().name();
        String primaryRole = resolvePrimaryRole(roles);
        String accessToken = jwtService.generateAccessToken(user, roles, approvalStatus);
        String refreshToken = jwtService.generateRefreshToken(user);
        String companyName = companyProfile == null ? null : companyProfile.getCompanyName();
        String role = primaryRole == null ? null : primaryRole.replace("ROLE_", "");
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.accessTokenExpirationSeconds(),
                role,
                primaryRole,
                approvalStatus,
                new AuthResponse.UserSummary(
                        user.getId(),
                        user.getEmail(),
                        "%s %s".formatted(user.getFirstName(), user.getLastName()).trim(),
                        companyName,
                        roles,
                        role,
                        primaryRole,
                        approvalStatus
                )
        );
    }

    private Set<String> resolveEffectiveRoles(User user) {
        LinkedHashSet<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(this::normalizeRoleName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (companyProfileRepository.findByUserId(user.getId()).isPresent()) {
            roles.remove("ROLE_ADMIN");
            roles.remove("ROLE_STUDENT");
            roles.add("ROLE_COMPANY");
        }
        return roles;
    }

    private String resolvePrimaryRole(Set<String> roles) {
        return roles.stream()
                .sorted(Comparator.comparingInt(role -> {
                    int index = ROLE_PRIORITY.indexOf(role);
                    return index >= 0 ? index : Integer.MAX_VALUE;
                }))
                .findFirst()
                .orElse(null);
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String[] splitFullName(String fullName) {
        String normalized = trimToNull(fullName);
        if (normalized == null) {
            return new String[]{"EduRite", "User"};
        }
        String[] parts = normalized.split("\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], "User"};
        }
        return parts;
    }
}
