package com.edurite.auth.controller;

import com.edurite.auth.dto.AuthResponse;
import com.edurite.auth.dto.CompanyRegisterRequest;
import com.edurite.auth.dto.LoginRequest;
import com.edurite.auth.dto.StudentRegisterRequest;
import com.edurite.auth.service.AuthService;
import com.edurite.company.dto.CompanyForgotPasswordRequest;
import com.edurite.company.dto.CompanyResetPasswordRequest;
import com.edurite.company.service.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/auth")
/**
 * This class named AuthController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final CompanyService companyService;

    public AuthController(AuthService authService, CompanyService companyService) {
        this.authService = authService;
        this.companyService = companyService;
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/register/student")
    /**
     * this method handles the "registerStudent" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<AuthResponse> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request));
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/register/company")
    /**
     * this method handles the "registerCompany" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<AuthResponse> registerCompany(@Valid @RequestBody CompanyRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCompany(request));
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/login")
    /**
     * this method handles the "login" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String normalizedEmail = request.email() == null ? null : request.email().trim().toLowerCase(Locale.ROOT);
        String requestedPortal = normalizePortal(servletRequest.getHeader("X-Auth-Portal"));
        String forwardedFor = trimToNull(servletRequest.getHeader("X-Forwarded-For"));
        String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : trimToNull(servletRequest.getRemoteAddr());
        String userAgent = trimToNull(servletRequest.getHeader("User-Agent"));

        log.info(
                "[auth] login http request email={} requestedPortal={} clientIp={} forwardedFor={} userAgent={} uri={}",
                normalizedEmail,
                requestedPortal,
                clientIp,
                forwardedFor,
                userAgent,
                servletRequest.getRequestURI()
        );

        AuthResponse response = authService.login(request);
        log.info(
                "[auth] login http response email={} requestedPortal={} resolvedPrimaryRole={} approvalStatus={}",
                normalizedEmail,
                requestedPortal,
                response.primaryRole(),
                response.user() == null ? null : response.user().approvalStatus()
        );
        return ResponseEntity.ok(response);
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/refresh")
    /**
     * this method handles the "refresh" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(authService.refresh(payload.get("refreshToken")));
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/logout")
    public Map<String, String> logout() { return Map.of("message", "Logout successful"); }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/forgot-password")
    /**
     * this method handles the "forgotPassword" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, String> forgotPassword(@RequestBody CompanyForgotPasswordRequest request) {
        return Map.of("message", companyService.issuePasswordResetToken(request));
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/reset-password")
    /**
     * this method handles the "resetPassword" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, String> resetPassword(@Valid @RequestBody CompanyResetPasswordRequest request) {
        companyService.resetPassword(request);
        return Map.of("message", "Password reset complete");
    }
    private String normalizePortal(String requestedPortal) {
        String normalized = trimToNull(requestedPortal);
        return normalized == null ? "UNKNOWN" : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

}
