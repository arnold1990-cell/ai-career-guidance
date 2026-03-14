package com.edurite.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Request payload for analysing multiple trusted university pages.
 */
public record UniversitySourcesAnalysisRequest(
        List<@Pattern(regexp = "^https?://.+", message = "Each URL must start with http:// or https://") String> urls,
        String targetProgram,
        String careerInterest,
        String qualificationLevel,
        @Max(value = 20, message = "maxRecommendations must be at most 20")
        Integer maxRecommendations
) {
}
