package com.edurite.ai.dto;

import java.util.List;

public record UniversitySourcesAnalysisResponse(
        Boolean aiLive,
        Boolean fallbackUsed,
        String warningMessage,
        List<String> sourceUrls,
        List<String> successfullyAnalysedUrls,
        List<String> failedUrls,
        Integer totalSourcesUsed,
        String summary,
        List<RecommendedCareer> recommendedCareers,
        List<RecommendedProgramme> recommendedProgrammes,
        List<String> recommendedUniversities,
        List<String> minimumRequirements,
        List<String> keyRequirements,
        List<String> skillGaps,
        List<String> recommendedNextSteps,
        List<String> warnings,
        Integer suitabilityScore,
        String rawModelUsed
) {

    public record RecommendedCareer(
            String name,
            String reason,
            List<String> requirements,
            List<String> relatedProgrammes
    ) {
    }

    public record RecommendedProgramme(
            String name,
            String university,
            List<String> admissionRequirements,
            String notes
    ) {
    }
}
