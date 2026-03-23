package com.edurite.auth.controller;

import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.CompanyRegisterRequest;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.dto.RegistrationResponse;
import com.edurite.auth.dto.StudentRegisterRequest;
import com.edurite.auth.dto.VerificationEmailRequest;
import com.edurite.auth.dto.VerificationStatusResponse;
import com.edurite.auth.service.AuthService;
import com.edurite.company.dto.CompanyForgotPasswordRequest;
import com.edurite.company.dto.CompanyResetPasswordRequest;
import com.edurite.company.service.CompanyService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CompanyService companyService;

    public AuthController(AuthService authService, CompanyService companyService) {
        this.authService = authService;
        this.companyService = companyService;
    }

    @PostMapping("/register/student")
    public ResponseEntity<RegistrationResponse> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request));
    }

    @PostMapping("/register/company")
    public ResponseEntity<RegistrationResponse> registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCompany(request));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<VerificationStatusResponse> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<VerificationStatusResponse> resendVerification(@Valid @RequestBody VerificationEmailRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(authService.refresh(payload.get("refreshToken")));
    }

    @PostMapping("/logout")
    public Map<String, String> logout() { return Map.of("message", "Logout successful"); }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody CompanyForgotPasswordRequest request) {
        return Map.of("message", companyService.issuePasswordResetToken(request));
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody CompanyResetPasswordRequest request) {
        companyService.resetPassword(request);
        return Map.of("message", "Password reset complete");
    }
}
