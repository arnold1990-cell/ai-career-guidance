package com.edurite.company.controller;

import com.edurite.company.dto.CompanyBursaryDto;
import com.edurite.company.dto.CompanyBursaryUpsertRequest;
import com.edurite.company.dto.CompanyDocumentDto;
import com.edurite.company.dto.CompanyProfileDto;
import com.edurite.company.dto.CompanyProfileUpdateRequest;
import com.edurite.company.dto.CompanyStudentSearchResultDto;
import com.edurite.company.service.CompanyService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/companies")
/**
 * This class named CompanyController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/me")
    /**
     * Beginner note: this method handles the "me" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto me(Principal principal) {
        return companyService.getMe(principal);
    }

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/me")
    /**
     * Beginner note: this method handles the "updateMe" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto updateMe(Principal principal, @Valid @org.springframework.web.bind.annotation.RequestBody CompanyProfileUpdateRequest request) {
        return companyService.updateMe(principal, request);
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/me/documents")
    /**
     * Beginner note: this method handles the "uploadDocument" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<CompanyDocumentDto> uploadDocument(Principal principal, @RequestPart("file") MultipartFile file, @RequestParam String documentType) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.uploadDocument(principal, file, documentType));
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/me/documents")
    /**
     * Beginner note: this method handles the "listDocuments" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyDocumentDto> listDocuments(Principal principal) {
        return companyService.listDocuments(principal);
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/bursaries")
    /**
     * Beginner note: this method handles the "createBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<CompanyBursaryDto> createBursary(Principal principal, @Valid @org.springframework.web.bind.annotation.RequestBody CompanyBursaryUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createBursary(principal, request));
    }

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/bursaries/{id}")
    /**
     * Beginner note: this method handles the "updateBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto updateBursary(Principal principal, @PathVariable UUID id, @Valid @org.springframework.web.bind.annotation.RequestBody CompanyBursaryUpsertRequest request) {
        return companyService.updateBursary(principal, id, request);
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/bursaries")
    /**
     * Beginner note: this method handles the "companyBursaries" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyBursaryDto> companyBursaries(Principal principal) {
        return companyService.listOwnBursaries(principal);
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/bursaries/{id}")
    /**
     * Beginner note: this method handles the "companyBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto companyBursary(Principal principal, @PathVariable UUID id) {
        return companyService.getOwnBursary(principal, id);
    }

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/bursaries/{id}/unpublish")
    /**
     * Beginner note: this method handles the "unpublishBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto unpublishBursary(Principal principal, @PathVariable UUID id) {
        return companyService.setBursaryStatus(principal, id, "UNPUBLISHED");
    }

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/bursaries/{id}/close")
    /**
     * Beginner note: this method handles the "closeBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto closeBursary(Principal principal, @PathVariable UUID id) {
        return companyService.setBursaryStatus(principal, id, "CLOSED");
    }

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/bursaries/{id}/reopen")
    /**
     * Beginner note: this method handles the "reopenBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto reopenBursary(Principal principal, @PathVariable UUID id) {
        return companyService.setBursaryStatus(principal, id, "ACTIVE");
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/students/search")
    public List<CompanyStudentSearchResultDto> searchStudents(
            Principal principal,
            @RequestParam(defaultValue = "") String fieldOfInterest,
            @RequestParam(defaultValue = "") String qualificationLevel,
            @RequestParam(defaultValue = "") String skills,
            @RequestParam(defaultValue = "") String location
    ) {
        return companyService.searchStudents(principal, fieldOfInterest, qualificationLevel, skills, location);
    }
}
