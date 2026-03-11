package com.edurite.application.controller;

import com.edurite.application.entity.ApplicationRecord;
import com.edurite.application.service.ApplicationService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1")
/**
 * This class named ApplicationController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

// @PostMapping handles HTTP POST requests for creating data.
    @PostMapping("/bursaries/{id}/applications")
    /**
     * Beginner note: this method handles the "apply" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public ApplicationRecord apply(@PathVariable UUID id, Principal principal) {
        return applicationService.submit(id, principal);
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/applications/me")
    /**
     * Beginner note: this method handles the "myApplications" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public List<ApplicationRecord> myApplications(Principal principal) {
        return applicationService.listMine(principal);
    }
}
