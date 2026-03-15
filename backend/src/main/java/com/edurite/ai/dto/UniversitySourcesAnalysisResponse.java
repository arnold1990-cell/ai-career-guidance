package com.edurite.ai.dto;

import java.util.List;

public record UniversitySourcesAnalysisResponse(
        List<String> sourceUrls,
        List<String> successfullyAnalysedUrls,
        List<String> failedUrls,
        Integer totalSourcesUsed,
        String summary,
        List<String> recommendedCareers,
        List<String> recommendedProgrammes,
        List<String> recommendedUniversities,
        List<String> keyRequirements,
        List<String> skillGaps,
        List<String> recommendedNextSteps,
        List<String> warnings,
        Integer suitabilityScore,
        String rawModelUsed
) {
}
