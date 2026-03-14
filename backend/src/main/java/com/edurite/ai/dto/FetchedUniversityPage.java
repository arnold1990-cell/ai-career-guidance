package com.edurite.ai.dto;

import java.util.List;

public record FetchedUniversityPage(
        String sourceUrl,
        String pageTitle,
        UniversityPageType pageType,
        String cleanedText,
        List<String> extractedKeywords,
        List<String> extractedNotes,
        boolean success,
        String error
) {
}
