package com.edurite.ai.university;

import java.util.List;
import java.util.Set;

public record UniversitySourcePageResult(
        String sourceUrl,
        String pageTitle,
        UniversityPageType pageType,
        String cleanedText,
        Set<String> extractedKeywords,
        List<String> extractedHeadings,
        boolean success,
        String failureReason,
        UniversityCrawlFailureType failureType
) {
}
