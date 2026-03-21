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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PublicUniversitySourceDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(PublicUniversitySourceDiscoveryService.class);

    private final UniversitySourceRegistryService registryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final UniversityUrlNormalizer urlNormalizer;
    private final UniversityRegistryProperties properties;

    public PublicUniversitySourceDiscoveryService(UniversitySourceRegistryService registryService,
                                                  MultiUniversityPageFetcherService pageFetcherService,
                                                  UniversityUrlNormalizer urlNormalizer,
                                                  UniversityRegistryProperties properties) {
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.urlNormalizer = urlNormalizer;
        this.properties = properties;
    }

    public List<String> discoverSources(StudentProfile profile, UniversitySourcesAnalysisRequest request, int maxUrls) {
        List<UniversityRegistryProperties.UniversityRegistryEntry> rankedUniversities = rankedUniversities(profile, request);
        Set<String> discovered = new LinkedHashSet<>();
        log.info("University source discovery starting: registrySize={}, rankedUniversities={}, requestedSources={}, usesDefaultSources={}",
                registryService.configuredUniversityCount(), rankedUniversities.size(), maxUrls, request.usesDefaultSources());

        for (UniversityRegistryProperties.UniversityRegistryEntry university : rankedUniversities) {
            discovered.addAll(university.getSeedUrls());
            List<String> candidateUrls = pageFetcherService.discoverCandidateUrls(university, Math.max(4, maxUrls / 2));
            if (candidateUrls.isEmpty()) {
                candidateUrls = fallbackToHomepages(university);
                log.warn("University discovery produced no crawler candidates; using homepage fallback: university={}, fallbackUrls={}",
                        university.getUniversityName(), candidateUrls.size());
            }
            discovered.addAll(candidateUrls);
            if (discovered.size() >= maxUrls) {
                break;
            }
        }

        List<String> normalized = discovered.stream()
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .filter(registryService::isAllowedUrl)
                .limit(maxUrls)
                .toList();

        if (normalized.isEmpty()) {
            normalized = registryService.getFallbackSources(maxUrls);
            log.warn("University source discovery returned zero URLs after filtering; falling back to registry seed URLs: fallbackUrls={}", normalized.size());
        }

        log.info("University source discovery completed: registrySize={}, rankedUniversities={}, discoveredUrls={}, requestedSources={}",
                registryService.configuredUniversityCount(), rankedUniversities.size(), normalized.size(), maxUrls);
        return normalized;
    }


    private List<String> fallbackToHomepages(UniversityRegistryProperties.UniversityRegistryEntry university) {
        return university.getSeedUrls().stream()
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .toList();
    }

    private List<UniversityRegistryProperties.UniversityRegistryEntry> rankedUniversities(StudentProfile profile,
                                                                                           UniversitySourcesAnalysisRequest request) {
        return registryService.getActiveUniversities().stream()
                .sorted(Comparator.comparingInt((UniversityRegistryProperties.UniversityRegistryEntry entry) -> relevanceScore(entry, profile, request)).reversed())
                .limit(Math.min(
                        Math.max(1, properties.getCrawl().getMaxUniversitiesPerRequest()),
                        registryService.configuredUniversityCount()))
                .toList();
    }

    private int relevanceScore(UniversityRegistryProperties.UniversityRegistryEntry university,
                               StudentProfile profile,
                               UniversitySourcesAnalysisRequest request) {
        String text = (university.getUniversityName() + " " + university.getBaseDomain() + " "
                + String.join(" ", university.getQualificationLevelsSupported())).toLowerCase(Locale.ROOT);
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
        for (String token : text.toLowerCase(Locale.ROOT).split("[,\\s/]+")) {
            if (token.length() >= 3) {
                values.put(token, Boolean.TRUE);
            }
        }
    }

}
