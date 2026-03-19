package com.edurite.student.controller;

import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.dto.StudentProfileUpsertRequest;
import com.edurite.student.dto.StudentSettingsDto;
import com.edurite.student.service.StudentService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/student")
/**
 * This class named StudentController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/profile")
    /**
     * this method handles the "profile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto profile(Principal principal) {
        return studentService.getProfile(principal);
    }

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/profile")
    /**
     * this method handles the "upsert" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto upsert(Principal principal, @Valid @org.springframework.web.bind.annotation.RequestBody StudentProfileUpsertRequest request) {
        return studentService.upsertProfile(principal, request);
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/profile/cv")
    /**
     * this method handles the "uploadCv" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto uploadCv(Principal principal, @RequestParam("file") MultipartFile file) throws IOException {
        return studentService.uploadDocument(principal, file, "cv");
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/profile/transcript")
    /**
     * this method handles the "uploadTranscript" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto uploadTranscript(Principal principal, @RequestParam("file") MultipartFile file) throws IOException {
        return studentService.uploadDocument(principal, file, "transcript");
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/dashboard")
    /**
     * this method handles the "dashboard" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> dashboard(Principal principal) {
        return studentService.dashboard(principal);
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/settings")
    /**
     * this method handles the "settings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto settings(Principal principal) {
        return studentService.getSettings(principal);
    }

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/settings")
    /**
     * this method handles the "updateSettings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto updateSettings(Principal principal, @org.springframework.web.bind.annotation.RequestBody StudentSettingsDto request) {
        return studentService.updateSettings(principal, request);
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/careers/{careerId}/save")
    /**
     * this method handles the "saveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, String>> saveCareer(Principal principal, @PathVariable UUID careerId) {
        studentService.saveCareer(principal, careerId);
        return ResponseEntity.ok(Map.of("message", "Career saved"));
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/bursaries/{bursaryId}/save")
    /**
     * this method handles the "saveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, String>> saveBursary(Principal principal, @PathVariable UUID bursaryId) {
        studentService.saveBursary(principal, bursaryId);
        return ResponseEntity.ok(Map.of("message", "Bursary saved"));
    }

// @DeleteMapping handles HTTP DELETE requests for deleting data.
    @DeleteMapping("/careers/{careerId}/save")
    /**
     * this method handles the "unsaveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, String>> unsaveCareer(Principal principal, @PathVariable UUID careerId) {
        studentService.unsaveCareer(principal, careerId);
        return ResponseEntity.ok(Map.of("message", "Career removed"));
    }

// @DeleteMapping handles HTTP DELETE requests for deleting data.
    @DeleteMapping("/bursaries/{bursaryId}/save")
    /**
     * this method handles the "unsaveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, String>> unsaveBursary(Principal principal, @PathVariable UUID bursaryId) {
        studentService.unsaveBursary(principal, bursaryId);
        return ResponseEntity.ok(Map.of("message", "Bursary removed"));
    }

// @GetMapping handles HTTP GET requests for reading data.
    @PostMapping("/opportunities/{opportunityType}/{opportunityId}/save")
    public ResponseEntity<Map<String, String>> saveOpportunity(Principal principal, @PathVariable String opportunityType, @PathVariable String opportunityId) {
        studentService.saveOpportunity(principal, opportunityType, opportunityId);
        return ResponseEntity.ok(Map.of("message", "Opportunity saved"));
    }

    @DeleteMapping("/opportunities/{opportunityType}/{opportunityId}/save")
    public ResponseEntity<Map<String, String>> unsaveOpportunity(Principal principal, @PathVariable String opportunityType, @PathVariable String opportunityId) {
        studentService.unsaveOpportunity(principal, opportunityType, opportunityId);
        return ResponseEntity.ok(Map.of("message", "Opportunity removed"));
    }

    @GetMapping("/opportunities/saved")
    public Map<String, Object> savedOpportunities(Principal principal) {
        return Map.of("items", studentService.savedOpportunityKeys(principal));
    }

    @GetMapping("/careers/saved")
    /**
     * this method handles the "savedCareers" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> savedCareers(Principal principal) {
        return Map.of("items", studentService.savedCareerIds(principal));
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/bursaries/bookmarks")
    public Map<String, Object> bookmarkedBursaries(Principal principal) {
        return Map.of("items", studentService.savedBursaries(principal));
    }

    @GetMapping("/bursaries/saved")
    /**
     * this method handles the "savedBursaries" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> savedBursaries(Principal principal) {
        return Map.of("items", studentService.savedBursaryIds(principal));
    }
}

