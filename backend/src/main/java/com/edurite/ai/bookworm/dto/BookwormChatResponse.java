package com.edurite.ai.bookworm.dto;

import java.util.List;

public record BookwormChatResponse(
        String answer,
        List<String> recommendedCareers,
        List<String> recommendedProgrammes,
        List<UniversityLink> recommendedUniversities,
        List<String> links
) {
    public record UniversityLink(String name, String website) {
    }
}
