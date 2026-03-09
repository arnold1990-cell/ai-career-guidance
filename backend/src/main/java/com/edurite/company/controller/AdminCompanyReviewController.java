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

@RestController
@RequestMapping("/api/v1/admin/companies")
public class AdminCompanyReviewController {

    private final CompanyService companyService;
    private final CurrentUserService currentUserService;

    public AdminCompanyReviewController(CompanyService companyService, CurrentUserService currentUserService) {
        this.companyService = companyService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/pending")
    public List<CompanyProfileDto> pending() {
        return companyService.listPendingCompanies();
    }

    @GetMapping("/{id}")
    public CompanyProfileDto details(@PathVariable UUID id) {
        return companyService.getCompanyById(id);
    }

    @PatchMapping("/{id}/approve")
    public CompanyProfileDto approve(@PathVariable UUID id, @Valid @RequestBody AdminCompanyReviewRequest request, Principal principal) {
        User admin = currentUserService.requireUser(principal);
        return companyService.approve(id, admin.getId(), request);
    }

    @PatchMapping("/{id}/reject")
    public CompanyProfileDto reject(@PathVariable UUID id, @Valid @RequestBody AdminCompanyReviewRequest request, Principal principal) {
        User admin = currentUserService.requireUser(principal);
        return companyService.reject(id, admin.getId(), request);
    }

    @PatchMapping("/{id}/more-info")
    public CompanyProfileDto moreInfo(@PathVariable UUID id, @Valid @RequestBody AdminCompanyReviewRequest request, Principal principal) {
        User admin = currentUserService.requireUser(principal);
        return companyService.requestMoreInfo(id, admin.getId(), request);
    }
}
