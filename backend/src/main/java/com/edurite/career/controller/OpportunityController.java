package com.edurite.career.controller;

import com.edurite.career.dto.OpportunityDto;
import com.edurite.career.service.OpportunityCatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final OpportunityCatalogService opportunityCatalogService;

    public OpportunityController(OpportunityCatalogService opportunityCatalogService) {
        this.opportunityCatalogService = opportunityCatalogService;
    }

    @GetMapping
    public List<OpportunityDto> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String field,
            @RequestParam(defaultValue = "") String industry,
            @RequestParam(defaultValue = "") String qualification,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String demand,
            @RequestParam(defaultValue = "ALL") String opportunityType
    ) {
        return opportunityCatalogService.search(q, field, industry, qualification, location, demand, opportunityType);
    }
}
