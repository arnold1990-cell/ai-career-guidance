package com.edurite.ai.university;

import java.util.List;

public record UniversitySourceDefinition(
        String universityName,
        String baseDomain,
        List<String> allowedDomains,
        List<String> seedUrls,
        List<String> qualificationLevelsSupported,
        boolean active,
        int crawlPriority
) {

    public UniversitySourceDefinition {
        allowedDomains = allowedDomains == null ? List.of() : List.copyOf(allowedDomains);
        seedUrls = seedUrls == null ? List.of() : List.copyOf(seedUrls);
        qualificationLevelsSupported = qualificationLevelsSupported == null
                ? List.of()
                : List.copyOf(qualificationLevelsSupported);
    }
}
