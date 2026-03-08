package com.edurite.company.controller;

import com.edurite.company.dto.CompanyProfileDto;
import com.edurite.company.service.CompanyService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/me")
    public CompanyProfileDto me(Principal principal) {
        return companyService.getMe(UUID.nameUUIDFromBytes(principal.getName().getBytes()));
    }

    @PutMapping("/me")
    public Map<String, String> updateMe(@RequestBody CompanyProfileDto dto) { return Map.of("message", "Company profile updated"); }

    @PostMapping("/me/documents")
    public Map<String, String> uploadDocument() { return Map.of("message", "Document accepted"); }

    @PostMapping("/bursaries")
    public Map<String, String> createBursary() { return Map.of("message", "Bursary created"); }

    @PutMapping("/bursaries/{id}")
    public Map<String, String> updateBursary() { return Map.of("message", "Bursary updated"); }

    @GetMapping("/bursaries")
    public List<Map<String, String>> companyBursaries() { return List.of(Map.of("name", "STEM Excellence 2026")); }

    @GetMapping("/applicants")
    public List<Map<String, String>> applicants() { return List.of(Map.of("applicant", "student@example.com")); }
}
