package com.edurite.ai.university;

import java.util.Set;

public record UniversitySourcePageResult(
        String sourceUrl,
        String pageTitle,
        UniversityPageType pageType,
        String cleanedText,
        Set<String> extractedKeywords,
        boolean success,
        String failureReason
) {
}
