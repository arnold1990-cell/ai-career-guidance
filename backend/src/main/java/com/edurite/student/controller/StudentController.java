package com.edurite.student.controller; // declares the package path for this Java file

import com.edurite.student.dto.StudentProfileDto; // imports a class so it can be used in this file
import com.edurite.student.dto.StudentProfileUpsertRequest; // imports a class so it can be used in this file
import com.edurite.student.dto.StudentSettingsDto; // imports a class so it can be used in this file
import com.edurite.student.service.StudentService; // imports a class so it can be used in this file
import jakarta.validation.Valid; // imports a class so it can be used in this file
import java.io.IOException; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.util.Map; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.http.ResponseEntity; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.DeleteMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PathVariable; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PostMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PutMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestParam; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file
import org.springframework.web.multipart.MultipartFile; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/student") // sets the base URL path for endpoints in this controller
/**
 * This class named StudentController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StudentController { // defines a class type

    private final StudentService studentService; // executes this statement as part of the application logic

    public StudentController(StudentService studentService) { // declares a method that defines behavior for this class
        this.studentService = studentService; // executes this statement as part of the application logic
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/profile") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "profile" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto profile(Principal principal) { // declares a method that defines behavior for this class
        return studentService.getProfile(principal); // returns a value from this method to the caller
    } // ends the current code block

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/profile") // maps this method to handle HTTP PUT requests
    /**
     * Note: this method handles the "upsert" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto upsert(Principal principal, @Valid @org.springframework.web.bind.annotation.RequestBody StudentProfileUpsertRequest request) { // declares a method that defines behavior for this class
        return studentService.upsertProfile(principal, request); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/profile/cv") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "uploadCv" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto uploadCv(Principal principal, @RequestParam("file") MultipartFile file) throws IOException { // declares a method that defines behavior for this class
        return studentService.uploadDocument(principal, file, "cv"); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/profile/transcript") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "uploadTranscript" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentProfileDto uploadTranscript(Principal principal, @RequestParam("file") MultipartFile file) throws IOException { // declares a method that defines behavior for this class
        return studentService.uploadDocument(principal, file, "transcript"); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/dashboard") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "dashboard" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> dashboard(Principal principal) { // declares a method that defines behavior for this class
        return studentService.dashboard(principal); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/settings") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "settings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto settings(Principal principal) { // declares a method that defines behavior for this class
        return studentService.getSettings(principal); // returns a value from this method to the caller
    } // ends the current code block

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/settings") // maps this method to handle HTTP PUT requests
    /**
     * Note: this method handles the "updateSettings" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public StudentSettingsDto updateSettings(Principal principal, @org.springframework.web.bind.annotation.RequestBody StudentSettingsDto request) { // declares a method that defines behavior for this class
        return studentService.updateSettings(principal, request); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/careers/{careerId}/save") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "saveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, String>> saveCareer(Principal principal, @PathVariable UUID careerId) { // declares a method that defines behavior for this class
        studentService.saveCareer(principal, careerId); // executes this statement as part of the application logic
        return ResponseEntity.ok(Map.of("message", "Career saved")); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/bursaries/{bursaryId}/save") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "saveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, String>> saveBursary(Principal principal, @PathVariable UUID bursaryId) { // declares a method that defines behavior for this class
        studentService.saveBursary(principal, bursaryId); // executes this statement as part of the application logic
        return ResponseEntity.ok(Map.of("message", "Bursary saved")); // returns a value from this method to the caller
    } // ends the current code block

// @DeleteMapping handles HTTP DELETE requests for deleting data.
    @DeleteMapping("/careers/{careerId}/save") // maps this method to handle HTTP DELETE requests
    /**
     * Note: this method handles the "unsaveCareer" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, String>> unsaveCareer(Principal principal, @PathVariable UUID careerId) { // declares a method that defines behavior for this class
        studentService.unsaveCareer(principal, careerId); // executes this statement as part of the application logic
        return ResponseEntity.ok(Map.of("message", "Career removed")); // returns a value from this method to the caller
    } // ends the current code block

// @DeleteMapping handles HTTP DELETE requests for deleting data.
    @DeleteMapping("/bursaries/{bursaryId}/save") // maps this method to handle HTTP DELETE requests
    /**
     * Note: this method handles the "unsaveBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<Map<String, String>> unsaveBursary(Principal principal, @PathVariable UUID bursaryId) { // declares a method that defines behavior for this class
        studentService.unsaveBursary(principal, bursaryId); // executes this statement as part of the application logic
        return ResponseEntity.ok(Map.of("message", "Bursary removed")); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/careers/saved") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "savedCareers" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> savedCareers(Principal principal) { // declares a method that defines behavior for this class
        return Map.of("items", studentService.savedCareerIds(principal)); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/bursaries/saved") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "savedBursaries" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public Map<String, Object> savedBursaries(Principal principal) { // declares a method that defines behavior for this class
        return Map.of("items", studentService.savedBursaryIds(principal)); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block

