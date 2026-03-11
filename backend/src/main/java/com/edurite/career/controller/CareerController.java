package com.edurite.career.controller;

import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/careers")
/**
 * This class named CareerController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CareerController {

    private final CareerRepository careerRepository;

    public CareerController(CareerRepository careerRepository) {
        this.careerRepository = careerRepository;
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping
    public Page<Career> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String field,
            @RequestParam(defaultValue = "") String industry,
            @RequestParam(defaultValue = "") String qualificationLevel,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String demand,
            @RequestParam(defaultValue = "") String salaryRange,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String industryFilter = !industry.isBlank() ? industry : field;
        return careerRepository.search(
                q,
                industryFilter,
                qualificationLevel,
                location,
                demand,
                salaryRange,
                PageRequest.of(page, size));
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/{id}")
    public Career get(@PathVariable UUID id) { return careerRepository.findById(id).orElseThrow(); }
}
