package com.edurite.admin.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named AdminService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AdminService {

    /**
     * this method handles the "users" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<Map<String, String>> users() {
        return List.of(Map.of("email", "student@example.com", "status", "ACTIVE"));
    }

    /**
     * this method handles the "analytics" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> analytics() {
        return Map.of("activeUsers", 1200, "applicationsSubmitted", 9400, "approvalRate", 0.61);
    }
}
