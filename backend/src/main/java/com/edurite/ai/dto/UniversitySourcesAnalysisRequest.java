package com.edurite.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UniversitySourcesAnalysisRequest(
        @Size(max = 10, message = "At most 10 URLs are allowed")
        List<String> urls,
        String targetProgram,
        String careerInterest,
        String qualificationLevel,
        @Max(value = 20, message = "maxRecommendations cannot exceed 20")
        Integer maxRecommendations
) {
    public int safeMaxRecommendations() {
        return maxRecommendations == null ? 10 : Math.max(1, Math.min(maxRecommendations, 20));
    }
}
