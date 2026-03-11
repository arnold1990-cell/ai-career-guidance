package com.edurite.company.controller;

import com.edurite.company.dto.AdminCompanyReviewRequest;
import com.edurite.company.dto.CompanyProfileDto;
import com.edurite.company.service.CompanyService;
import com.edurite.security.service.CurrentUserService;
import com.edurite.user.entity.User;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/admin/companies")
/**
 * This class named AdminCompanyReviewController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class AdminCompanyReviewController {

    private final CompanyService companyService;
    private final CurrentUserService currentUserService;

    public AdminCompanyReviewController(CompanyService companyService, CurrentUserService currentUserService) {
        this.companyService = companyService;
        this.currentUserService = currentUserService;
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/pending")
    /**
     * this method handles the "pending" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<CompanyProfileDto> pending() {
        return companyService.listPendingCompanies();
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/{id}")
    /**
     * this method handles the "details" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto details(@PathVariable UUID id) {
        return companyService.getCompanyById(id);
    }

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/{id}/approve")
    /**
     * this method handles the "approve" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto approve(@PathVariable UUID id, @Valid @RequestBody AdminCompanyReviewRequest request, Principal principal) {
        User admin = currentUserService.requireUser(principal);
        return companyService.approve(id, admin.getId(), request);
    }

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/{id}/reject")
    /**
     * this method handles the "reject" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto reject(@PathVariable UUID id, @Valid @RequestBody AdminCompanyReviewRequest request, Principal principal) {
        User admin = currentUserService.requireUser(principal);
        return companyService.reject(id, admin.getId(), request);
    }

// @PatchMapping handles HTTP PATCH requests for partial updates.
    @PatchMapping("/{id}/more-info")
    /**
     * this method handles the "moreInfo" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto moreInfo(@PathVariable UUID id, @Valid @RequestBody AdminCompanyReviewRequest request, Principal principal) {
        User admin = currentUserService.requireUser(principal);
        return companyService.requestMoreInfo(id, admin.getId(), request);
    }
}
