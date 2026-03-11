package com.edurite.admin.controller; // declares the package path for this Java file

import com.edurite.admin.service.AdminService; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PatchMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PostMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PutMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/admin") // sets the base URL path for endpoints in this controller
/**
 * This class named AdminController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AdminController { // defines a class type

    private final AdminService adminService; // executes this statement as part of the application logic

    public AdminController(AdminService adminService) { // declares a method that defines behavior for this class
        this.adminService = adminService; // executes this statement as part of the application logic
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/users") // maps this method to handle HTTP GET requests
    public List<Map<String, String>> users() { return adminService.users(); } // declares a method that defines behavior for this class

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/users/{id}/status") // maps this method to handle HTTP PATCH requests
    public Map<String, String> userStatus() { return Map.of("message", "User status updated"); } // declares a method that defines behavior for this class

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/roles") // maps this method to handle HTTP GET requests
    public List<Map<String, String>> roles() { return List.of(Map.of("name", "ADMIN"), Map.of("name", "STUDENT")); } // declares a method that defines behavior for this class

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/roles") // maps this method to handle HTTP POST requests
    public Map<String, String> createRole() { return Map.of("message", "Role created"); } // declares a method that defines behavior for this class

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/roles/{id}") // maps this method to handle HTTP PUT requests
    public Map<String, String> updateRole() { return Map.of("message", "Role updated"); } // declares a method that defines behavior for this class

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/bursaries/pending") // maps this method to handle HTTP GET requests
    public List<Map<String, String>> pendingBursaries() { return List.of(Map.of("title", "STEM Excellence 2026")); } // declares a method that defines behavior for this class

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/bursaries/{id}/review") // maps this method to handle HTTP PATCH requests
    public Map<String, String> reviewBursary() { return Map.of("message", "Review recorded"); } // declares a method that defines behavior for this class

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/analytics") // maps this method to handle HTTP GET requests
    public Map<String, Object> analytics() { return adminService.analytics(); } // declares a method that defines behavior for this class
} // ends the current code block
