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

@RestController
@RequestMapping("/api/v1/bursaries")
public class BursaryController {

    private final BursaryRepository bursaryRepository;

    public BursaryController(BursaryRepository bursaryRepository) {
        this.bursaryRepository = bursaryRepository;
    }

    @GetMapping
    public Page<Bursary> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String qualificationLevel,
            @RequestParam(defaultValue = "") String region,
            @RequestParam(defaultValue = "") String eligibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bursaryRepository.findByTitleContainingIgnoreCaseAndQualificationLevelContainingIgnoreCaseAndRegionContainingIgnoreCaseAndEligibilityContainingIgnoreCase(
                q, qualificationLevel, region, eligibility, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public Bursary get(@PathVariable UUID id) { return bursaryRepository.findById(id).orElseThrow(); }
}
