package com.edurite.student.controller;

import com.edurite.student.dto.StudentProfileDto;
import com.edurite.student.dto.StudentProfileUpsertRequest;
import com.edurite.student.service.StudentService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/me")
    public StudentProfileDto profile(Principal principal) {
        return studentService.getProfile(principal);
    }

    @PutMapping("/me")
    public StudentProfileDto upsert(Principal principal, @Valid @org.springframework.web.bind.annotation.RequestBody StudentProfileUpsertRequest request) {
        return studentService.upsertProfile(principal, request);
    }

    @PostMapping("/me/upload")
    public StudentProfileDto upload(Principal principal, @RequestParam("file") MultipartFile file, @RequestParam("type") String type) throws IOException {
        return studentService.uploadDocument(principal, file, type);
    }

    @GetMapping("/me/dashboard")
    public Map<String, Object> dashboard(Principal principal) {
        return studentService.dashboard(principal);
    }

    @PostMapping("/me/saved-careers/{careerId}")
    public ResponseEntity<Map<String, String>> saveCareer(Principal principal, @PathVariable UUID careerId) {
        studentService.saveCareer(principal, careerId);
        return ResponseEntity.ok(Map.of("message", "Career saved"));
    }

    @PostMapping("/me/saved-bursaries/{bursaryId}")
    public ResponseEntity<Map<String, String>> saveBursary(Principal principal, @PathVariable UUID bursaryId) {
        studentService.saveBursary(principal, bursaryId);
        return ResponseEntity.ok(Map.of("message", "Bursary saved"));
    }
}
