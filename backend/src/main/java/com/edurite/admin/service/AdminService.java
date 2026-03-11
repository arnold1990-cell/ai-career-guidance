package com.edurite.admin.service; // declares the package path for this Java file

import java.util.List; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import org.springframework.stereotype.Service; // imports a class so it can be used in this file

// @Service marks a class that contains business logic.
@Service // marks this class as a service containing business logic
/**
 * This class named AdminService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AdminService { // defines a class type

    /**
     * Note: this method handles the "users" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<Map<String, String>> users() { // declares a method that defines behavior for this class
        return List.of(Map.of("email", "student@example.com", "status", "ACTIVE")); // returns a value from this method to the caller
    } // ends the current code block

    /**
     * Note: this method handles the "analytics" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> analytics() { // declares a method that defines behavior for this class
        return Map.of("activeUsers", 1200, "applicationsSubmitted", 9400, "approvalRate", 0.61); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
