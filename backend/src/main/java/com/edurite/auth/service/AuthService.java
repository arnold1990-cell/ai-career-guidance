package com.edurite.auth.service; // declares the package path for this Java file

import com.edurite.auth.dto.AuthResponse; // imports a class so it can be used in this file
import com.edurite.auth.dto.CompanyRegisterRequest; // imports a class so it can be used in this file
import com.edurite.auth.dto.LoginRequest; // imports a class so it can be used in this file
import com.edurite.auth.dto.StudentRegisterRequest; // imports a class so it can be used in this file
import com.edurite.common.exception.DuplicateEmailException; // imports a class so it can be used in this file
import com.edurite.common.exception.InvalidCredentialsException; // imports a class so it can be used in this file
import com.edurite.common.exception.ResourceConflictException; // imports a class so it can be used in this file
import com.edurite.company.entity.CompanyApprovalStatus; // imports a class so it can be used in this file
import com.edurite.company.entity.CompanyProfile; // imports a class so it can be used in this file
import com.edurite.company.repository.CompanyProfileRepository; // imports a class so it can be used in this file
import com.edurite.security.service.JwtService; // imports a class so it can be used in this file
import com.edurite.student.entity.StudentProfile; // imports a class so it can be used in this file
import com.edurite.student.repository.StudentProfileRepository; // imports a class so it can be used in this file
import com.edurite.user.entity.Role; // imports a class so it can be used in this file
import com.edurite.user.entity.User; // imports a class so it can be used in this file
import com.edurite.user.entity.UserStatus; // imports a class so it can be used in this file
import com.edurite.user.repository.RoleRepository; // imports a class so it can be used in this file
import com.edurite.user.repository.UserRepository; // imports a class so it can be used in this file
import java.util.Locale; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.security.authentication.AuthenticationManager; // imports a class so it can be used in this file
import org.springframework.security.authentication.BadCredentialsException; // imports a class so it can be used in this file
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // imports a class so it can be used in this file
import org.springframework.security.core.Authentication; // imports a class so it can be used in this file
import org.springframework.security.core.userdetails.UserDetails; // imports a class so it can be used in this file
import org.springframework.security.crypto.password.PasswordEncoder; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file
import org.springframework.transaction.annotation.Transactional; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named AuthService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AuthService { // defines a class type

    private final UserRepository userRepository; // reads or writes data through the database layer
    private final RoleRepository roleRepository; // reads or writes data through the database layer
    private final StudentProfileRepository studentProfileRepository; // reads or writes data through the database layer
    private final CompanyProfileRepository companyProfileRepository; // reads or writes data through the database layer
    private final PasswordEncoder passwordEncoder; // handles authentication or authorization to protect secure access
    private final JwtService jwtService; // handles authentication or authorization to protect secure access
    private final AuthenticationManager authenticationManager; // executes this statement as part of the application logic

    public AuthService( // supports the surrounding application logic
            UserRepository userRepository, // reads or writes data through the database layer
            RoleRepository roleRepository, // reads or writes data through the database layer
            StudentProfileRepository studentProfileRepository, // reads or writes data through the database layer
            CompanyProfileRepository companyProfileRepository, // reads or writes data through the database layer
            PasswordEncoder passwordEncoder, // handles authentication or authorization to protect secure access
            JwtService jwtService, // handles authentication or authorization to protect secure access
            AuthenticationManager authenticationManager // supports the surrounding application logic
    ) { // supports the surrounding application logic
        this.userRepository = userRepository; // reads or writes data through the database layer
        this.roleRepository = roleRepository; // reads or writes data through the database layer
        this.studentProfileRepository = studentProfileRepository; // reads or writes data through the database layer
        this.companyProfileRepository = companyProfileRepository; // reads or writes data through the database layer
        this.passwordEncoder = passwordEncoder; // handles authentication or authorization to protect secure access
        this.jwtService = jwtService; // handles authentication or authorization to protect secure access
        this.authenticationManager = authenticationManager; // executes this statement as part of the application logic
    } // ends the current code block

    @Transactional // wraps this method in a database transaction for safe commit or rollback
    /**
     * Note: this method handles the "registerStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public AuthResponse registerStudent(StudentRegisterRequest request) { // declares a method that defines behavior for this class
        String[] names = splitFullName(request.fullName()); // executes this statement as part of the application logic
        User user = createUser(request.email(), request.password(), names[0], names[1], "ROLE_STUDENT"); // executes this statement as part of the application logic

        StudentProfile profile = new StudentProfile(); // creates a new object instance and stores it in a variable
        profile.setUserId(user.getId()); // executes this statement as part of the application logic
        studentProfileRepository.save(profile); // reads or writes data through the database layer

        return toAuthResponse(user); // returns a value from this method to the caller
    } // ends the current code block

    @Transactional // wraps this method in a database transaction for safe commit or rollback
    /**
     * Note: this method handles the "registerCompany" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public AuthResponse registerCompany(CompanyRegisterRequest request) { // declares a method that defines behavior for this class
        String officialEmail = request.officialEmail(); // executes this statement as part of the application logic
        if (officialEmail == null || officialEmail.isBlank()) { // checks a condition and runs this block only when true
            officialEmail = request.email(); // executes this statement as part of the application logic
        } // ends the current code block
        officialEmail = normalizeEmail(officialEmail); // executes this statement as part of the application logic

        if (officialEmail == null || officialEmail.isBlank()) { // checks a condition and runs this block only when true
            throw new ResourceConflictException("Company email is required"); // throws an exception to signal an error condition
        } // ends the current code block

        String companyName = request.companyName() == null ? "" : request.companyName().trim(); // executes this statement as part of the application logic
        if (companyName.isBlank()) { // checks a condition and runs this block only when true
            throw new ResourceConflictException("Company name is required"); // throws an exception to signal an error condition
        } // ends the current code block

        String contactPersonName = request.contactPersonName(); // executes this statement as part of the application logic
        if (contactPersonName == null || contactPersonName.isBlank()) { // checks a condition and runs this block only when true
            contactPersonName = companyName; // executes this statement as part of the application logic
        } else { // supports the surrounding application logic
            contactPersonName = contactPersonName.trim(); // executes this statement as part of the application logic
        } // ends the current code block

        String registrationNumber = request.registrationNumber(); // executes this statement as part of the application logic
        if (registrationNumber == null || registrationNumber.isBlank()) { // checks a condition and runs this block only when true
            registrationNumber = "PENDING-" + UUID.randomUUID(); // executes this statement as part of the application logic
        } else { // supports the surrounding application logic
            registrationNumber = registrationNumber.trim(); // executes this statement as part of the application logic
        } // ends the current code block

        if (companyProfileRepository.existsByRegistrationNumberIgnoreCase(registrationNumber)) { // checks a condition and runs this block only when true
            throw new ResourceConflictException("Company registration number already exists"); // throws an exception to signal an error condition
        } // ends the current code block

        User user = createUser( // supports the surrounding application logic
                officialEmail, // supports the surrounding application logic
                request.password(), // supports the surrounding application logic
                contactPersonName, // supports the surrounding application logic
                companyName, // supports the surrounding application logic
                "ROLE_COMPANY" // supports the surrounding application logic
        ); // executes this statement as part of the application logic

        CompanyProfile profile = new CompanyProfile(); // creates a new object instance and stores it in a variable
        profile.setUserId(user.getId()); // executes this statement as part of the application logic
        profile.setCompanyName(companyName); // executes this statement as part of the application logic
        profile.setRegistrationNumber(registrationNumber); // executes this statement as part of the application logic
        profile.setIndustry(request.industry()); // executes this statement as part of the application logic
        profile.setOfficialEmail(officialEmail); // executes this statement as part of the application logic
        profile.setMobileNumber(request.mobileNumber()); // executes this statement as part of the application logic
        profile.setContactPersonName(contactPersonName); // executes this statement as part of the application logic
        profile.setAddress(request.address()); // executes this statement as part of the application logic
        profile.setWebsite(request.website()); // executes this statement as part of the application logic
        profile.setDescription(request.description()); // executes this statement as part of the application logic
        profile.setStatus(CompanyApprovalStatus.PENDING); // executes this statement as part of the application logic
        companyProfileRepository.save(profile); // reads or writes data through the database layer

        return toAuthResponse(user); // returns a value from this method to the caller
    } // ends the current code block


    /**
     * Note: this method handles the "refresh" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public AuthResponse refresh(String refreshToken) { // handles authentication or authorization to protect secure access
        if (refreshToken == null || refreshToken.isBlank()) { // checks a condition and runs this block only when true
            throw new InvalidCredentialsException(); // throws an exception to signal an error condition
        } // ends the current code block

        String username; // executes this statement as part of the application logic
        try { // supports the surrounding application logic
            if (!jwtService.isRefreshToken(refreshToken)) { // checks a condition and runs this block only when true
                throw new InvalidCredentialsException(); // throws an exception to signal an error condition
            } // ends the current code block
            username = jwtService.extractUsername(refreshToken); // handles authentication or authorization to protect secure access
        } catch (RuntimeException ex) { // supports the surrounding application logic
            throw new InvalidCredentialsException(); // throws an exception to signal an error condition
        } // ends the current code block

        User user = userRepository.findByEmail(username).orElseThrow(InvalidCredentialsException::new); // reads or writes data through the database layer
        org.springframework.security.core.userdetails.UserDetails userDetails = org.springframework.security.core.userdetails.User // supports the surrounding application logic
                .withUsername(user.getEmail()) // supports the surrounding application logic
                .password(user.getPasswordHash()) // supports the surrounding application logic
                .disabled(user.getStatus() != UserStatus.ACTIVE) // supports the surrounding application logic
                .authorities(user.getRoles().stream().map(Role::getName).toArray(String[]::new)) // supports the surrounding application logic
                .build(); // executes this statement as part of the application logic

        if (!jwtService.isTokenValid(refreshToken, userDetails)) { // checks a condition and runs this block only when true
            throw new InvalidCredentialsException(); // throws an exception to signal an error condition
        } // ends the current code block

        return toAuthResponse(user); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "login" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public AuthResponse login(LoginRequest request) { // declares a method that defines behavior for this class
        try { // supports the surrounding application logic
            Authentication authentication = authenticationManager.authenticate( // handles authentication or authorization to protect secure access
                    new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()) // handles authentication or authorization to protect secure access
            ); // executes this statement as part of the application logic
            UserDetails userDetails = (UserDetails) authentication.getPrincipal(); // executes this statement as part of the application logic
            User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(InvalidCredentialsException::new); // reads or writes data through the database layer
            return toAuthResponse(user); // returns a value from this method to the caller
        } catch (BadCredentialsException ex) { // supports the surrounding application logic
            throw new InvalidCredentialsException(); // throws an exception to signal an error condition
        } // ends the current code block
    } // ends the current code block

    /**
     * Note: this method handles the "createUser" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private User createUser(String email, String password, String firstName, String lastName, String roleName) { // declares a method that defines behavior for this class
        String normalizedEmail = normalizeEmail(email); // executes this statement as part of the application logic
        if (userRepository.existsByEmail(normalizedEmail)) { // checks a condition and runs this block only when true
            throw new DuplicateEmailException(normalizedEmail); // throws an exception to signal an error condition
        } // ends the current code block

        Role role = roleRepository.findByName(roleName) // reads or writes data through the database layer
                .orElseThrow(() -> new ResourceConflictException("Role '%s' does not exist".formatted(roleName))); // executes this statement as part of the application logic

        User user = new User(); // creates a new object instance and stores it in a variable
        user.setEmail(normalizedEmail); // executes this statement as part of the application logic
        user.setPasswordHash(passwordEncoder.encode(password)); // handles authentication or authorization to protect secure access
        user.setFirstName(firstName.trim()); // executes this statement as part of the application logic
        user.setLastName(lastName.trim()); // executes this statement as part of the application logic
        user.setStatus(UserStatus.ACTIVE); // executes this statement as part of the application logic
        user.getRoles().add(role); // executes this statement as part of the application logic
        return userRepository.save(user); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "toAuthResponse" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private AuthResponse toAuthResponse(User user) { // declares a method that defines behavior for this class
        String accessToken = jwtService.generateAccessToken(user); // handles authentication or authorization to protect secure access
        String refreshToken = jwtService.generateRefreshToken(user); // handles authentication or authorization to protect secure access
        return new AuthResponse( // returns a value from this method to the caller
                accessToken, // handles authentication or authorization to protect secure access
                refreshToken, // handles authentication or authorization to protect secure access
                "Bearer", // supports the surrounding application logic
                jwtService.accessTokenExpirationSeconds(), // handles authentication or authorization to protect secure access
                new AuthResponse.UserSummary( // supports the surrounding application logic
                        user.getId(), // supports the surrounding application logic
                        user.getEmail(), // supports the surrounding application logic
                        "%s %s".formatted(user.getFirstName(), user.getLastName()).trim(), // supports the surrounding application logic
                        user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()) // supports the surrounding application logic
                ) // supports the surrounding application logic
        ); // executes this statement as part of the application logic
    } // ends the current code block

    /**
     * Note: this method handles the "normalizeEmail" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String normalizeEmail(String email) { // declares a method that defines behavior for this class
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "splitFullName" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String[] splitFullName(String fullName) { // declares a method that defines behavior for this class
        String trimmed = fullName.trim(); // executes this statement as part of the application logic
        String[] parts = trimmed.split("\\s+", 2); // executes this statement as part of the application logic
        if (parts.length == 1) { // checks a condition and runs this block only when true
            return new String[]{parts[0], parts[0]}; // returns a value from this method to the caller
        } // ends the current code block
        return parts; // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block