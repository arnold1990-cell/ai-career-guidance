package com.edurite.admin.controller;

import com.edurite.admin.service.AdminService;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String accountType
    ) {
        return adminService.users(search, status, accountType);
    }

    @PatchMapping("/users/{id}/status")
    public Map<String, Object> userStatus(@PathVariable UUID id, @RequestBody Map<String, Boolean> payload, Principal principal) {
        return adminService.updateUserStatus(id, Boolean.TRUE.equals(payload.get("active")), principal);
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> roles() {
        return adminService.roles();
    }

    @PostMapping("/roles")
    public Map<String, Object> createRole(@RequestBody Map<String, Object> payload, Principal principal) {
        return adminService.createRole(payload, principal);
    }

    @PutMapping("/roles/{id}")
    public Map<String, Object> updateRole(@PathVariable UUID id, @RequestBody Map<String, Object> payload, Principal principal) {
        return adminService.updateRole(id, payload, principal);
    }

    @DeleteMapping("/roles/{id}")
    public Map<String, String> deleteRole(@PathVariable UUID id, Principal principal) {
        return adminService.deleteRole(id, principal);
    }

    @GetMapping("/bursaries/pending")
    public List<Map<String, Object>> pendingBursaries() {
        return adminService.pendingBursaries();
    }

    @PatchMapping("/bursaries/{id}/review")
    public Map<String, Object> reviewBursary(@PathVariable UUID id, @RequestBody Map<String, String> payload, Principal principal) {
        return adminService.reviewBursary(id, payload.get("decision"), payload.get("comment"), principal);
    }

    @PostMapping("/users/bulk-upload")
    public Map<String, Object> bulkUpload(@RequestPart("file") MultipartFile file, Principal principal) throws IOException {
        return adminService.bulkUploadUsers(file, principal);
    }

    @GetMapping("/audit-logs")
    public List<Map<String, Object>> auditLogs() {
        return adminService.auditLogs();
    }

    @GetMapping("/analytics")
    public Map<String, Object> analytics() {
        return adminService.analytics();
    }
}
