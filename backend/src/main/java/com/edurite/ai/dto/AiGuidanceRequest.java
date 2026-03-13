package com.edurite.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload used by /api/v1/ai/guidance.
 *
 * Fields are optional except firstName to support partial profile submissions.
 */
public record AiGuidanceRequest(
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 1000) String interests,
        @Size(max = 1000) String skills,
        @Size(max = 120) String qualificationLevel,
        @Size(max = 120) String location,
        @Size(max = 2000) String bio,
        @Size(max = 1200) String careerGoals,
        @Size(max = 1000) String academicStrengths,
        @Size(max = 1000) String weakAreas
) {
}
