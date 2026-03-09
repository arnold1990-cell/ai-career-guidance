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

@RestController
@RequestMapping("/api/v1/student")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/profile")
    public StudentProfileDto profile(Principal principal) {
        return studentService.getProfile(principal);
    }

    @PutMapping("/profile")
    public StudentProfileDto upsert(Principal principal, @Valid @org.springframework.web.bind.annotation.RequestBody StudentProfileUpsertRequest request) {
        return studentService.upsertProfile(principal, request);
    }

    @PostMapping("/profile/cv")
    public StudentProfileDto uploadCv(Principal principal, @RequestParam("file") MultipartFile file) throws IOException {
        return studentService.uploadDocument(principal, file, "cv");
    }

    @PostMapping("/profile/transcript")
    public StudentProfileDto uploadTranscript(Principal principal, @RequestParam("file") MultipartFile file) throws IOException {
        return studentService.uploadDocument(principal, file, "transcript");
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Principal principal) {
        return studentService.dashboard(principal);
    }

    @GetMapping("/settings")
    public StudentSettingsDto settings(Principal principal) {
        return studentService.getSettings(principal);
    }

    @PutMapping("/settings")
    public StudentSettingsDto updateSettings(Principal principal, @org.springframework.web.bind.annotation.RequestBody StudentSettingsDto request) {
        return studentService.updateSettings(principal, request);
    }

    @PostMapping("/careers/{careerId}/save")
    public ResponseEntity<Map<String, String>> saveCareer(Principal principal, @PathVariable UUID careerId) {
        studentService.saveCareer(principal, careerId);
        return ResponseEntity.ok(Map.of("message", "Career saved"));
    }

    @PostMapping("/bursaries/{bursaryId}/save")
    public ResponseEntity<Map<String, String>> saveBursary(Principal principal, @PathVariable UUID bursaryId) {
        studentService.saveBursary(principal, bursaryId);
        return ResponseEntity.ok(Map.of("message", "Bursary saved"));
    }

    @DeleteMapping("/careers/{careerId}/save")
    public ResponseEntity<Map<String, String>> unsaveCareer(Principal principal, @PathVariable UUID careerId) {
        studentService.unsaveCareer(principal, careerId);
        return ResponseEntity.ok(Map.of("message", "Career removed"));
    }

    @DeleteMapping("/bursaries/{bursaryId}/save")
    public ResponseEntity<Map<String, String>> unsaveBursary(Principal principal, @PathVariable UUID bursaryId) {
        studentService.unsaveBursary(principal, bursaryId);
        return ResponseEntity.ok(Map.of("message", "Bursary removed"));
    }

    @GetMapping("/careers/saved")
    public Map<String, Object> savedCareers(Principal principal) {
        return Map.of("items", studentService.savedCareerIds(principal));
    }

    @GetMapping("/bursaries/saved")
    public Map<String, Object> savedBursaries(Principal principal) {
        return Map.of("items", studentService.savedBursaryIds(principal));
    }
}

