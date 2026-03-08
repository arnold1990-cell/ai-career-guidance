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

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<Map<String, String>> users() { return adminService.users(); }

    @PatchMapping("/users/{id}/status")
    public Map<String, String> userStatus() { return Map.of("message", "User status updated"); }

    @GetMapping("/roles")
    public List<Map<String, String>> roles() { return List.of(Map.of("name", "ADMIN"), Map.of("name", "STUDENT")); }

    @PostMapping("/roles")
    public Map<String, String> createRole() { return Map.of("message", "Role created"); }

    @PutMapping("/roles/{id}")
    public Map<String, String> updateRole() { return Map.of("message", "Role updated"); }

    @GetMapping("/bursaries/pending")
    public List<Map<String, String>> pendingBursaries() { return List.of(Map.of("title", "STEM Excellence 2026")); }

    @PatchMapping("/bursaries/{id}/review")
    public Map<String, String> reviewBursary() { return Map.of("message", "Review recorded"); }

    @GetMapping("/analytics")
    public Map<String, Object> analytics() { return adminService.analytics(); }
}
