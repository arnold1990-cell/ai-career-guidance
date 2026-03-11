package com.edurite.admin.controller;

import com.edurite.admin.service.AdminService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/admin")
/**
 * This class named AdminController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/users")
    public List<Map<String, String>> users() { return adminService.users(); }

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/users/{id}/status")
    public Map<String, String> userStatus() { return Map.of("message", "User status updated"); }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/roles")
    public List<Map<String, String>> roles() { return List.of(Map.of("name", "ADMIN"), Map.of("name", "STUDENT")); }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/roles")
    public Map<String, String> createRole() { return Map.of("message", "Role created"); }

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/roles/{id}")
    public Map<String, String> updateRole() { return Map.of("message", "Role updated"); }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/bursaries/pending")
    public List<Map<String, String>> pendingBursaries() { return List.of(Map.of("title", "STEM Excellence 2026")); }

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/bursaries/{id}/review")
    public Map<String, String> reviewBursary() { return Map.of("message", "Review recorded"); }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/analytics")
    public Map<String, Object> analytics() { return adminService.analytics(); }
}
