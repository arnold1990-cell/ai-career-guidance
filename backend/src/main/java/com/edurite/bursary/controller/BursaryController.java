package com.edurite.bursary.controller;

import com.edurite.bursary.entity.Bursary;
import com.edurite.bursary.repository.BursaryRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/bursaries")
/**
 * This class named BursaryController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class BursaryController {

    private final BursaryRepository bursaryRepository;

    public BursaryController(BursaryRepository bursaryRepository) {
        this.bursaryRepository = bursaryRepository;
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping
    public Page<Bursary> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String qualificationLevel,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String eligibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bursaryRepository.findByTitleContainingIgnoreCaseAndQualificationLevelContainingIgnoreCaseAndLocationContainingIgnoreCaseAndEligibilityContainingIgnoreCase(
                q, qualificationLevel, location, eligibility, PageRequest.of(page, size));
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/{id}")
    public Bursary get(@PathVariable UUID id) { return bursaryRepository.findById(id).orElseThrow(); }
}
