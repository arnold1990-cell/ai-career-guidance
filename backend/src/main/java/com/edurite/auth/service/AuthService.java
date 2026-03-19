package com.edurite.auth.service;

import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.CompanyRegisterRequest;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.dto.StudentRegisterRequest;
import com.edurite.common.exception.DuplicateEmailException;
import com.edurite.common.exception.InvalidCredentialsException;
import com.edurite.common.exception.ResourceConflictException;
import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.security.service.JwtService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.repository.StudentProfileRepository;
import com.edurite.user.entity.Role;
import com.edurite.user.entity.User;
import com.edurite.user.entity.UserStatus;
import com.edurite.user.repository.RoleRepository;
import com.edurite.user.repository.UserRepository;
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

// @Service marks a class that contains business logic.
@Service
/**
 * This class named AuthService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final List<String> ROLE_PRIORITY = List.of("ROLE_ADMIN", "ROLE_COMPANY", "ROLE_STUDENT");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            CompanyProfileRepository companyProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    /**
     * this method handles the "registerStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public AuthResponse registerStudent(StudentRegisterRequest request) {
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
                "ROLE_STUDENT"
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

        return toAuthResponse(user);
    }

    @Transactional
    /**
     * this method handles the "registerCompany" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public AuthResponse registerCompany(CompanyRegisterRequest request) {
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
                "ROLE_COMPANY"
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

        return toAuthResponse(user);
    }


    /**
     * this method handles the "refresh" step of the feature.
     * It exists to keep this class focused and reusable.
     */
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
        org.springframework.security.core.userdetails.UserDetails userDetails = org.springframework.security.core.userdetails.User
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

    /**
     * this method handles the "login" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        Optional<User> candidateUser = userRepository.findByEmail(normalizedEmail);
        boolean passwordMatches = candidateUser
                .map(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElse(false);
        log.info(
                "[auth] login requested email={} userFound={} passwordMatches={}",
                normalizedEmail,
                candidateUser.isPresent(),
                passwordMatches
        );
        candidateUser.ifPresent(user -> {
            Set<String> databaseRoles = user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            CompanyApprovalStatus companyApprovalStatus = companyProfileRepository.findByUserId(user.getId())
                    .map(CompanyProfile::getStatus)
                    .orElse(null);
            log.info(
                    "[auth] login candidate email={} dbRoles={} status={} companyApprovalStatus={} passwordHash={}",
                    user.getEmail(),
                    databaseRoles,
                    user.getStatus(),
                    companyApprovalStatus,
                    user.getPasswordHash()
            );
        });
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(InvalidCredentialsException::new);
            Set<String> databaseRoles = user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            log.info("[auth] authenticated username={} dbRoles={}", user.getEmail(), databaseRoles);
            return toAuthResponse(user);
        } catch (BadCredentialsException ex) {
            log.warn("[auth] failed login attempt username={} reason=bad_credentials", normalizedEmail, ex);
            throw new InvalidCredentialsException();
        } catch (DisabledException ex) {
            log.warn("[auth] failed login attempt username={} reason=disabled", normalizedEmail, ex);
            throw new InvalidCredentialsException();
        } catch (RuntimeException ex) {
            log.error("[auth] failed login attempt username={} reason=unexpected_exception", normalizedEmail, ex);
            throw ex;
        }
    }

    /**
     * this method handles the "createUser" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private User createUser(String email, String password, String firstName, String lastName, String roleName) {
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
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    /**
     * this method handles the "toAuthResponse" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private AuthResponse toAuthResponse(User user) {
        Set<String> roles = resolveEffectiveRoles(user);
        CompanyProfile companyProfile = companyProfileRepository.findByUserId(user.getId()).orElse(null);
        String approvalStatus = companyProfile == null ? null : companyProfile.getStatus().name();
        String primaryRole = resolvePrimaryRole(roles);
        String accessToken = jwtService.generateAccessToken(user, roles, approvalStatus);
        String refreshToken = jwtService.generateRefreshToken(user);
        String companyName = companyProfile == null ? null : companyProfile.getCompanyName();
        String role = primaryRole == null ? null : primaryRole.replace("ROLE_", "");
        log.info("[auth] login response username={} responseRole={} responseRoles={} approvalStatus={} payloadUserRole={}", user.getEmail(), primaryRole, roles, approvalStatus, role);
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.accessTokenExpirationSeconds(),
                role,
                primaryRole,
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
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(this::normalizeRoleName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        CompanyProfile companyProfile = companyProfileRepository.findByUserId(user.getId()).orElse(null);
        if (companyProfile != null) {
            roles.remove("ROLE_ADMIN");
            roles.remove("ROLE_STUDENT");
            roles.add("ROLE_COMPANY");
            log.info(
                    "[auth] effective company role email={} dbRoles={} effectiveRoles={} approvalStatus={}",
                    user.getEmail(),
                    user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
                    roles,
                    companyProfile.getStatus()
            );
        }
        return roles;
    }

    private String resolvePrimaryRole(Set<String> roles) {
        return ROLE_PRIORITY.stream()
                .filter(roles::contains)
                .findFirst()
                .orElseGet(() -> roles.stream().map(this::normalizeRoleName).sorted(Comparator.naturalOrder()).findFirst().orElse(null));
    }

    private String normalizeRoleName(String roleName) {
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    /**
     * this method handles the "normalizeEmail" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * this method handles the "splitFullName" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String[] splitFullName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        if (trimmed.isBlank()) {
            return new String[]{"Student", "Student"};
        }
        String[] parts = trimmed.split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], parts[0]};
        }
        return parts;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
