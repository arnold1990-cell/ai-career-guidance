package com.edurite.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// @RestController tells Spring this class exposes REST API endpoints.
@RestControllerAdvice
/**
 * This class named ApiExceptionHandler is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldMessage)
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler({DuplicateEmailException.class, ResourceConflictException.class})
    /**
     * Beginner note: this method handles the "handleConflict" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class})
    /**
     * Beginner note: this method handles the "handleUnauthorized" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, Object>> handleUnauthorized(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Beginner note: this method handles the "toFieldMessage" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String toFieldMessage(FieldError fieldError) {
        return "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage());
    }

    /**
     * Beginner note: this method handles the "build" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}
