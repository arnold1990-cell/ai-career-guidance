package com.edurite.ai.university;

import java.util.List;

public record UniversitySourceDefinition(
        String universityKey,
        String displayName,
        String baseDomain,
        List<String> allowedDomains,
        List<String> officialHomepages,
        List<String> programmePages,
        List<String> admissionsPages,
        List<String> facultyPages,
        List<String> discoveryPages,
        List<String> sitemapUrls,
        List<String> blockedPatterns,
        List<String> allowedPatterns,
        List<String> qualificationLevelsSupported,
        SourcePriority sourcePriority,
        int crawlPriority,
        int maxPagesToFetch,
        boolean active
) {
    public List<String> seedUrls() {
        return java.util.stream.Stream.of(officialHomepages, programmePages, admissionsPages, facultyPages, discoveryPages)
                .flatMap(List::stream)
                .distinct()
                .toList();
    }
}
