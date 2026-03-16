package com.edurite.ai.dto;

import java.util.List;

public record CareerAdviceResponse(
        List<RecommendedCareer> recommendedCareers,
        List<RecommendedProgramme> recommendedProgrammes,
        List<RecommendedUniversity> recommendedUniversities,
        List<String> entryRequirements,
        List<String> skillGaps,
        List<String> nextSteps
) {

    public record RecommendedCareer(
            String name,
            Integer matchScore,
            String reason,
            List<String> improvements
    ) {
    }

    public record RecommendedProgramme(
            String name,
            String university,
            List<String> admissionRequirements
    ) {
    }

    public record RecommendedUniversity(
            String name,
            String website
    ) {
    }
}
