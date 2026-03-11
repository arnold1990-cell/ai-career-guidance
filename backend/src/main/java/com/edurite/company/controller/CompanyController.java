package com.edurite.company.controller; // declares the package path for this Java file

import com.edurite.company.dto.CompanyBursaryDto; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyBursaryUpsertRequest; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyDocumentDto; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyProfileDto; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyProfileUpdateRequest; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyStudentSearchResultDto; // imports a class so it can be used in this file
import com.edurite.company.service.CompanyService; // imports a class so it can be used in this file
import jakarta.validation.Valid; // imports a class so it can be used in this file
import java.io.IOException; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.http.HttpStatus; // imports a class so it can be used in this file
import org.springframework.http.ResponseEntity; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PatchMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PathVariable; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PostMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PutMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestParam; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestPart; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file
import org.springframework.web.multipart.MultipartFile; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/companies") // sets the base URL path for endpoints in this controller
/**
 * This class named CompanyController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CompanyController { // defines a class type

    private final CompanyService companyService; // executes this statement as part of the application logic

    public CompanyController(CompanyService companyService) { // declares a method that defines behavior for this class
        this.companyService = companyService; // executes this statement as part of the application logic
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/me") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "me" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto me(Principal principal) { // declares a method that defines behavior for this class
        return companyService.getMe(principal); // returns a value from this method to the caller
    } // ends the current code block

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/me") // maps this method to handle HTTP PUT requests
    /**
     * Note: this method handles the "updateMe" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto updateMe(Principal principal, @Valid @org.springframework.web.bind.annotation.RequestBody CompanyProfileUpdateRequest request) { // declares a method that defines behavior for this class
        return companyService.updateMe(principal, request); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/me/documents") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "uploadDocument" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<CompanyDocumentDto> uploadDocument(Principal principal, @RequestPart("file") MultipartFile file, @RequestParam String documentType) throws IOException { // declares a method that defines behavior for this class
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.uploadDocument(principal, file, documentType)); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/me/documents") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "listDocuments" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyDocumentDto> listDocuments(Principal principal) { // declares a method that defines behavior for this class
        return companyService.listDocuments(principal); // returns a value from this method to the caller
    } // ends the current code block

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/bursaries") // maps this method to handle HTTP POST requests
    /**
     * Note: this method handles the "createBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ResponseEntity<CompanyBursaryDto> createBursary(Principal principal, @Valid @org.springframework.web.bind.annotation.RequestBody CompanyBursaryUpsertRequest request) { // declares a method that defines behavior for this class
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createBursary(principal, request)); // returns a value from this method to the caller
    } // ends the current code block

// @PutMapping handles HTTP PUT requests for updating data.
    @PutMapping("/bursaries/{id}") // maps this method to handle HTTP PUT requests
    /**
     * Note: this method handles the "updateBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto updateBursary(Principal principal, @PathVariable UUID id, @Valid @org.springframework.web.bind.annotation.RequestBody CompanyBursaryUpsertRequest request) { // declares a method that defines behavior for this class
        return companyService.updateBursary(principal, id, request); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/bursaries") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "companyBursaries" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyBursaryDto> companyBursaries(Principal principal) { // declares a method that defines behavior for this class
        return companyService.listOwnBursaries(principal); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/bursaries/{id}") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "companyBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto companyBursary(Principal principal, @PathVariable UUID id) { // declares a method that defines behavior for this class
        return companyService.getOwnBursary(principal, id); // returns a value from this method to the caller
    } // ends the current code block

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/bursaries/{id}/unpublish") // maps this method to handle HTTP PATCH requests
    /**
     * Note: this method handles the "unpublishBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto unpublishBursary(Principal principal, @PathVariable UUID id) { // declares a method that defines behavior for this class
        return companyService.setBursaryStatus(principal, id, "UNPUBLISHED"); // returns a value from this method to the caller
    } // ends the current code block

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/bursaries/{id}/close") // maps this method to handle HTTP PATCH requests
    /**
     * Note: this method handles the "closeBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto closeBursary(Principal principal, @PathVariable UUID id) { // declares a method that defines behavior for this class
        return companyService.setBursaryStatus(principal, id, "CLOSED"); // returns a value from this method to the caller
    } // ends the current code block

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/bursaries/{id}/reopen") // maps this method to handle HTTP PATCH requests
    /**
     * Note: this method handles the "reopenBursary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyBursaryDto reopenBursary(Principal principal, @PathVariable UUID id) { // declares a method that defines behavior for this class
        return companyService.setBursaryStatus(principal, id, "ACTIVE"); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/students/search") // maps this method to handle HTTP GET requests
    public List<CompanyStudentSearchResultDto> searchStudents( // supports the surrounding application logic
            Principal principal, // supports the surrounding application logic
            @RequestParam(defaultValue = "") String fieldOfInterest, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String qualificationLevel, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String skills, // binds a query parameter value to this method parameter
            @RequestParam(defaultValue = "") String location // binds a query parameter value to this method parameter
    ) { // supports the surrounding application logic
        return companyService.searchStudents(principal, fieldOfInterest, qualificationLevel, skills, location); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
