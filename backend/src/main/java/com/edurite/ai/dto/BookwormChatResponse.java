package com.edurite.ai.dto;

import java.util.List;

public record BookwormChatResponse(
        String answerText,
        List<String> recommendedCareers,
        List<String> recommendedProgrammes,
        List<RecommendedUniversity> recommendedUniversities,
        List<String> universityWebsites,
        List<String> bursarySuggestions,
        List<String> roadmapSteps,
        List<String> warnings,
        String source
) {
    public record RecommendedUniversity(
            String name,
            String location,
            String officialWebsite,
            String category,
            List<String> programmes,
            List<String> entryRequirements,
            String source
    ) {
    }
}
