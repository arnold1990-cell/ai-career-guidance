package com.edurite.ai.dto;

import java.util.List;

public record CareerAdviceResponse(List<RecommendedCareer> recommendedCareers) {

    public record RecommendedCareer(
            String name,
            int matchScore,
            String reason,
            List<String> improvements
    ) {
    }
}
