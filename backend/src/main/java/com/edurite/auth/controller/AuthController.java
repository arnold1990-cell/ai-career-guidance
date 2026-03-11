package com.edurite.auth.controller; // declares the package path for this Java file

import com.edurite.auth.dto.AuthResponse; // imports a class so it can be used in this file
import com.edurite.auth.dto.CompanyRegisterRequest; // imports a class so it can be used in this file
import com.edurite.auth.dto.LoginRequest; // imports a class so it can be used in this file
import com.edurite.auth.dto.StudentRegisterRequest; // imports a class so it can be used in this file
import com.edurite.auth.service.AuthService; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyForgotPasswordRequest; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyResetPasswordRequest; // imports a class so it can be used in this file
import com.edurite.company.service.CompanyService; // imports a class so it can be used in this file
import jakarta.validation.Valid; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import org.springframework.http.HttpStatus; // imports a class so it can be used in this file
import org.springframework.http.ResponseEntity; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PostMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestBody; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/auth") // sets the base URL path for endpoints in this controller
/**
 * This class named AuthController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AuthController { // defines a class type

    private final AuthService authService; // executes this statement as part of the application logic
    private final CompanyService companyService; // executes this statement as part of the application logic

    public AuthController(AuthService authService, CompanyService companyService) { // declares a method that defines behavior for this class
        this.authService = authService; // executes this statement as part of the application logic
        this.companyService = companyService; // executes this statement as part of the application logic
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/register/student") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "registerStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<AuthResponse> registerStudent(@Valid @RequestBody StudentRegisterRequest request) { // declares a method that defines behavior for this class
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request)); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/register/company") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "registerCompany" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<AuthResponse> registerCompany(@Valid @RequestBody CompanyRegisterRequest request) { // declares a method that defines behavior for this class
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCompany(request)); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/login") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "login" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) { // declares a method that defines behavior for this class
        return ResponseEntity.ok(authService.login(request)); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/refresh") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "refresh" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> payload) { // declares a method that defines behavior for this class
        return ResponseEntity.ok(authService.refresh(payload.get("refreshToken"))); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/logout") // maps this method to handle HTTP POST requests
    public Map<String, String> logout() { return Map.of("message", "Logout successful"); } // declares a method that defines behavior for this class

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/forgot-password") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "forgotPassword" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, String> forgotPassword(@RequestBody CompanyForgotPasswordRequest request) { // declares a method that defines behavior for this class
        return Map.of("message", companyService.issuePasswordResetToken(request)); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/reset-password") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "resetPassword" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, String> resetPassword(@Valid @RequestBody CompanyResetPasswordRequest request) { // declares a method that defines behavior for this class
        companyService.resetPassword(request); // executes this statement as part of the application logic
        return Map.of("message", "Password reset complete"); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
