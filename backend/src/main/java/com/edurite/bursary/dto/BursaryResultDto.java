package com.edurite.bursary.dto;

import java.time.LocalDate;

public record BursaryResultDto(
        String externalId,
        String title,
        String provider,
        String description,
        String qualificationLevel,
        String region,
        String eligibility,
        LocalDate deadline,
        String applicationLink,
        String sourceType,
        int relevanceScore
) {}
