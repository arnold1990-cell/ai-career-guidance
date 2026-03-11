package com.edurite.common.exception; // declares the package path for this Java file

import jakarta.servlet.http.HttpServletRequest; // imports a class so it can be used in this file
import java.time.Instant; // imports a class so it can be used in this file
import java.util.LinkedHashMap; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import java.util.stream.Collectors; // imports a class so it can be used in this file
import org.springframework.http.HttpStatus; // imports a class so it can be used in this file
import org.springframework.http.ResponseEntity; // imports a class so it can be used in this file
import org.springframework.security.core.AuthenticationException; // imports a class so it can be used in this file
import org.springframework.validation.FieldError; // imports a class so it can be used in this file
import org.springframework.web.bind.MethodArgumentNotValidException; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.ExceptionHandler; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestControllerAdvice; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestControllerAdvice // adds metadata that Spring or Java uses at runtime
/**
 * This class named ApiExceptionHandler is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class ApiExceptionHandler { // defines a class type

    @ExceptionHandler(MethodArgumentNotValidException.class) // adds metadata that Spring or Java uses at runtime
    public ResponseEntity<Map<String, Object>> handleValidation( // supports the surrounding application logic
            MethodArgumentNotValidException ex, // supports the surrounding application logic
            HttpServletRequest request // supports the surrounding application logic
    ) { // supports the surrounding application logic
        String message = ex.getBindingResult().getFieldErrors().stream() // supports the surrounding application logic
                .map(this::toFieldMessage) // supports the surrounding application logic
                .collect(Collectors.joining(", ")); // executes this statement as part of the application logic
        return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI()); // returns a value from this method to the caller
    } // ends the current code block

    @ExceptionHandler({DuplicateEmailException.class, ResourceConflictException.class}) // adds metadata that Spring or Java uses at runtime
    /**
     * Note: this method handles the "handleConflict" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex, HttpServletRequest request) { // declares a method that defines behavior for this class
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI()); // returns a value from this method to the caller
    } // ends the current code block

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class}) // adds metadata that Spring or Java uses at runtime
    /**
     * Note: this method handles the "handleUnauthorized" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, Object>> handleUnauthorized(RuntimeException ex, HttpServletRequest request) { // declares a method that defines behavior for this class
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "toFieldMessage" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String toFieldMessage(FieldError fieldError) { // declares a method that defines behavior for this class
        return "%s: %s".formatted(fieldError.getField(), fieldError.getDefaultMessage()); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "build" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, String path) { // declares a method that defines behavior for this class
        Map<String, Object> body = new LinkedHashMap<>(); // creates a new object instance and stores it in a variable
        body.put("timestamp", Instant.now().toString()); // executes this statement as part of the application logic
        body.put("status", status.value()); // executes this statement as part of the application logic
        body.put("error", status.getReasonPhrase()); // executes this statement as part of the application logic
        body.put("message", message); // executes this statement as part of the application logic
        body.put("path", path); // executes this statement as part of the application logic
        return ResponseEntity.status(status).body(body); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
