package com.edurite.company.controller; // declares the package path for this Java file

import com.edurite.company.dto.AdminCompanyReviewRequest; // imports a class so it can be used in this file
import com.edurite.company.dto.CompanyProfileDto; // imports a class so it can be used in this file
import com.edurite.company.service.CompanyService; // imports a class so it can be used in this file
import com.edurite.security.service.CurrentUserService; // imports a class so it can be used in this file
import com.edurite.user.entity.User; // imports a class so it can be used in this file
import jakarta.validation.Valid; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PathVariable; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.PatchMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestBody; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/admin/companies") // sets the base URL path for endpoints in this controller
/**
 * This class named AdminCompanyReviewController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AdminCompanyReviewController { // defines a class type

    private final CompanyService companyService; // executes this statement as part of the application logic
    private final CurrentUserService currentUserService; // executes this statement as part of the application logic

    public AdminCompanyReviewController(CompanyService companyService, CurrentUserService currentUserService) { // declares a method that defines behavior for this class
        this.companyService = companyService; // executes this statement as part of the application logic
        this.currentUserService = currentUserService; // executes this statement as part of the application logic
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/pending") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "pending" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyProfileDto> pending() { // declares a method that defines behavior for this class
        return companyService.listPendingCompanies(); // returns a value from this method to the caller
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/{id}") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "details" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto details(@PathVariable UUID id) { // declares a method that defines behavior for this class
        return companyService.getCompanyById(id); // returns a value from this method to the caller
    } // ends the current code block

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/{id}/approve") // maps this method to handle HTTP PATCH requests
    /**
     * Note: this method handles the "approve" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto approve(@PathVariable UUID id, @Valid @RequestBody AdminCompanyReviewRequest request, Principal principal) { // declares a method that defines behavior for this class
        User admin = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        return companyService.approve(id, admin.getId(), request); // returns a value from this method to the caller
    } // ends the current code block

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/{id}/reject") // maps this method to handle HTTP PATCH requests
    /**
     * Note: this method handles the "reject" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto reject(@PathVariable UUID id, @Valid @RequestBody AdminCompanyReviewRequest request, Principal principal) { // declares a method that defines behavior for this class
        User admin = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        return companyService.reject(id, admin.getId(), request); // returns a value from this method to the caller
    } // ends the current code block

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/{id}/more-info") // maps this method to handle HTTP PATCH requests
    /**
     * Note: this method handles the "moreInfo" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto moreInfo(@PathVariable UUID id, @Valid @RequestBody AdminCompanyReviewRequest request, Principal principal) { // declares a method that defines behavior for this class
        User admin = currentUserService.requireUser(principal); // executes this statement as part of the application logic
        return companyService.requestMoreInfo(id, admin.getId(), request); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
