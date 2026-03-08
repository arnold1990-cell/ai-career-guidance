package com.edurite.admin.service;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    public List<Map<String, String>> users() {
        return List.of(Map.of("email", "student@example.com", "status", "ACTIVE"));
    }

    public Map<String, Object> analytics() {
        return Map.of("activeUsers", 1200, "applicationsSubmitted", 9400, "approvalRate", 0.61);
    }
}
