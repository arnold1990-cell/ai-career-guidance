package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PublicUniversitySourceDiscoveryService {

    private final UniversitySourceRegistryService registryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final UniversityUrlNormalizer urlNormalizer;

    public PublicUniversitySourceDiscoveryService(UniversitySourceRegistryService registryService,
                                                  MultiUniversityPageFetcherService pageFetcherService,
                                                  UniversityUrlNormalizer urlNormalizer) {
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.urlNormalizer = urlNormalizer;
    }

    public List<String> discoverSources(StudentProfile profile, UniversitySourcesAnalysisRequest request, int maxUrls) {
        Set<String> discovered = new LinkedHashSet<>();
        for (UniversityRegistryProperties.UniversityRegistryEntry university : rankedUniversities(profile, request)) {
            discovered.addAll(registryService.officialEntryPoints(university));
            discovered.addAll(pageFetcherService.discoverCandidateUrls(university, Math.min(maxUrls, university.getMaxPagesToFetch())));
            if (discovered.size() >= maxUrls) {
                break;
            }
        }
        return discovered.stream()
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .filter(registryService::isAllowedUrl)
                .limit(maxUrls)
                .toList();
    }

    private List<UniversityRegistryProperties.UniversityRegistryEntry> rankedUniversities(StudentProfile profile,
                                                                                           UniversitySourcesAnalysisRequest request) {
        int requestedInstitutions = Math.min(Math.max(4, request.safeMaxRecommendations() * 2), registryService.configuredInstitutionCount());
        return registryService.getActiveUniversities().stream()
                .sorted(Comparator.comparingInt((UniversityRegistryProperties.UniversityRegistryEntry entry) -> relevanceScore(entry, profile, request)).reversed()
                        .thenComparingInt(UniversityRegistryProperties.UniversityRegistryEntry::getCrawlPriority))
                .limit(requestedInstitutions)
                .toList();
    }

    private int relevanceScore(UniversityRegistryProperties.UniversityRegistryEntry university,
                               StudentProfile profile,
                               UniversitySourcesAnalysisRequest request) {
        String text = String.join(" ", List.of(
                university.getUniversityName(),
                university.getInstitutionType(),
                university.getProvince(),
                university.getBaseDomain(),
                String.join(" ", university.getQualificationLevelsSupported())
        )).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : tokens(profile, request)) {
            if (text.contains(token)) {
                score += 2;
            }
        }
        if (request.qualificationLevel() != null && university.getQualificationLevelsSupported().stream()
                .anyMatch(level -> level.equalsIgnoreCase(request.qualificationLevel()))) {
            score += 4;
        }
        if ("engineering".equalsIgnoreCase(request.careerInterest()) || normalize(request.careerInterest()).contains("engineering")) {
            if (normalize(String.join(" ", university.getProgrammePages())).contains("engineering")
                    || normalize(String.join(" ", university.getFacultyPages())).contains("engineering")) {
                score += 5;
            }
        }
        return score;
    }

    private List<String> tokens(StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        addTokens(values, request.targetProgram());
        addTokens(values, request.careerInterest());
        addTokens(values, request.qualificationLevel());
        addTokens(values, profile.getInterests());
        addTokens(values, profile.getSkills());
        addTokens(values, profile.getLocation());
        return new ArrayList<>(values.keySet());
    }

    private void addTokens(Map<String, Boolean> values, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String token : normalize(text).split("[,\\s/]+")) {
            if (token.length() >= 3) {
                values.put(token, Boolean.TRUE);
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
