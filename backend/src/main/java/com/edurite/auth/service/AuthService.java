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
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
     * Beginner note: this method handles the "registerStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public AuthResponse registerStudent(StudentRegisterRequest request) {
        String[] names = splitFullName(request.fullName());
        User user = createUser(request.email(), request.password(), names[0], names[1], "ROLE_STUDENT");

        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        studentProfileRepository.save(profile);

        return toAuthResponse(user);
    }

    @Transactional
    /**
     * Beginner note: this method handles the "registerCompany" step of the feature.
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
     * Beginner note: this method handles the "refresh" step of the feature.
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
        org.springframework.security.core.userdetails.UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(user.getStatus() != UserStatus.ACTIVE)
                .authorities(user.getRoles().stream().map(Role::getName).toArray(String[]::new))
                .build();

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new InvalidCredentialsException();
        }

        return toAuthResponse(user);
    }

    /**
     * Beginner note: this method handles the "login" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password())
            );
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(InvalidCredentialsException::new);
            return toAuthResponse(user);
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }
    }

    /**
     * Beginner note: this method handles the "createUser" step of the feature.
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
     * Beginner note: this method handles the "toAuthResponse" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private AuthResponse toAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.accessTokenExpirationSeconds(),
                new AuthResponse.UserSummary(
                        user.getId(),
                        user.getEmail(),
                        "%s %s".formatted(user.getFirstName(), user.getLastName()).trim(),
                        user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet())
                )
        );
    }

    /**
     * Beginner note: this method handles the "normalizeEmail" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Beginner note: this method handles the "splitFullName" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String[] splitFullName(String fullName) {
        String trimmed = fullName.trim();
        String[] parts = trimmed.split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], parts[0]};
        }
        return parts;
    }
}