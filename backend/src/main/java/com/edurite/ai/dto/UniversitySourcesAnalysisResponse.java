package com.edurite.ai.dto;

import java.util.List;

public record UniversitySourcesAnalysisResponse(
        Boolean aiLive,
        Boolean fallbackUsed,
        String mode,
        String groundingStatus,
        Integer evidenceCoverage,
        String warningMessage,
        List<String> requestedSources,
        List<String> sourceUrls,
        List<String> successfullyAnalysedUrls,
        List<String> failedUrls,
        Integer totalSourcesUsed,
        String summary,
        List<String> inferredGuidance,
        List<RecommendedCareer> recommendedCareers,
        List<RecommendedProgramme> recommendedProgrammes,
        List<RecommendedBursary> bursarySuggestions,
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

    public record RecommendedBursary(
            String name,
            String provider,
            List<String> eligibility,
            String notes,
            List<String> sourceUrls,
            boolean officialSource
    ) {
    }
}
