package com.edurite.security.config; // declares the package path for this Java file

import com.fasterxml.jackson.databind.ObjectMapper; // imports a class so it can be used in this file
import jakarta.servlet.http.HttpServletRequest; // imports a class so it can be used in this file
import jakarta.servlet.http.HttpServletResponse; // imports a class so it can be used in this file
import java.io.IOException; // imports a class so it can be used in this file
import java.time.Instant; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import org.springframework.http.HttpStatus; // imports a class so it can be used in this file
import org.springframework.http.MediaType; // imports a class so it can be used in this file
import org.springframework.security.access.AccessDeniedException; // imports a class so it can be used in this file
import org.springframework.security.web.access.AccessDeniedHandler; // imports a class so it can be used in this file
import org.springframework.stereotype.Component; // imports a class so it can be used in this file

@Component // marks this class as a Spring-managed component bean
/**
 * This class named RestAccessDeniedHandler is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class RestAccessDeniedHandler implements AccessDeniedHandler { // defines a class type

    private final ObjectMapper objectMapper; // executes this statement as part of the application logic

    public RestAccessDeniedHandler(ObjectMapper objectMapper) { // declares a method that defines behavior for this class
        this.objectMapper = objectMapper; // executes this statement as part of the application logic
    } // ends the current code block

    @Override // adds metadata that Spring or Java uses at runtime
    public void handle( // supports the surrounding application logic
            HttpServletRequest request, // supports the surrounding application logic
            HttpServletResponse response, // supports the surrounding application logic
            AccessDeniedException accessDeniedException // supports the surrounding application logic
    ) throws IOException { // supports the surrounding application logic
        response.setStatus(HttpStatus.FORBIDDEN.value()); // executes this statement as part of the application logic
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // executes this statement as part of the application logic

        Map<String, Object> body = Map.of( // supports the surrounding application logic
                "timestamp", Instant.now().toString(), // supports the surrounding application logic
                "status", HttpStatus.FORBIDDEN.value(), // supports the surrounding application logic
                "error", "Forbidden", // supports the surrounding application logic
                "message", "You do not have permission to access this resource", // supports the surrounding application logic
                "path", request.getRequestURI() // supports the surrounding application logic
        ); // executes this statement as part of the application logic

        objectMapper.writeValue(response.getOutputStream(), body); // executes this statement as part of the application logic
    } // ends the current code block
} // ends the current code block
