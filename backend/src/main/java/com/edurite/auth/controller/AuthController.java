package com.edurite.auth.controller;

import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.CompanyRegisterRequest;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.dto.StudentRegisterRequest;
import com.edurite.auth.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/student")
    public ResponseEntity<AuthResponse> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request));
    }

    @PostMapping("/register/company")
    public ResponseEntity<AuthResponse> registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCompany(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public Map<String, String> refresh() { return Map.of("message", "Refresh token endpoint placeholder"); }

    @PostMapping("/logout")
    public Map<String, String> logout() { return Map.of("message", "Logout successful"); }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword() { return Map.of("message", "Password reset initiated"); }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword() { return Map.of("message", "Password reset complete"); }
}
