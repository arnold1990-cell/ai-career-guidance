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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, StudentProfileRepository studentProfileRepository, CompanyProfileRepository companyProfileRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse registerStudent(StudentRegisterRequest request) {
        String[] names = splitFullName(request.fullName());
        User user = createUser(request.email(), request.password(), names[0], names[1], "ROLE_STUDENT");

        StudentProfile profile = new StudentProfile();
        profile.setUserId(user.getId());
        studentProfileRepository.save(profile);

        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse registerCompany(CompanyRegisterRequest request) {
        if (companyProfileRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber().trim())) {
            throw new ResourceConflictException("Company registration number already exists");
        }

        User user = createUser(request.officialEmail(), request.password(), request.contactPersonName(), request.companyName(), "ROLE_COMPANY");

        CompanyProfile profile = new CompanyProfile();
        profile.setUserId(user.getId());
        profile.setCompanyName(request.companyName().trim());
        profile.setRegistrationNumber(request.registrationNumber().trim());
        profile.setIndustry(request.industry());
        profile.setOfficialEmail(normalizeEmail(request.officialEmail()));
        profile.setMobileNumber(request.mobileNumber());
        profile.setContactPersonName(request.contactPersonName());
        profile.setAddress(request.address());
        profile.setWebsite(request.website());
        profile.setDescription(request.description());
        profile.setStatus(CompanyApprovalStatus.PENDING);
        companyProfileRepository.save(profile);

        return toAuthResponse(user);
    }

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

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String[] splitFullName(String fullName) {
        String trimmed = fullName.trim();
        String[] parts = trimmed.split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], parts[0]};
        }
        return parts;
    }
}
