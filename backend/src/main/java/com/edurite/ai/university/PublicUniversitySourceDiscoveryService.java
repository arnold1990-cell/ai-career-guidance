package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
        for (UniversitySourceDefinition university : rankedUniversities(profile, request)) {
            discovered.addAll(pageFetcherService.discoverCandidateUrls(university, Math.min(maxUrls, university.maxPagesToFetch())));
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

    private List<UniversitySourceDefinition> rankedUniversities(StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        return registryService.getActiveDefinitions().stream()
                .sorted(Comparator.comparingInt((UniversitySourceDefinition entry) -> relevanceScore(entry, profile, request)).reversed()
                        .thenComparing(UniversitySourceDefinition::crawlPriority))
                .limit(Math.min(Math.max(1, request.safeMaxRecommendations() * 4), registryService.configuredUniversityCount()))
                .toList();
    }

    private int relevanceScore(UniversitySourceDefinition university,
                               StudentProfile profile,
                               UniversitySourcesAnalysisRequest request) {
        String text = (university.displayName() + " " + university.baseDomain() + " "
                + String.join(" ", university.qualificationLevelsSupported())).toLowerCase(Locale.ROOT);
        int score = switch (university.sourcePriority()) {
            case CRITICAL -> 6;
            case HIGH -> 4;
            case STANDARD -> 2;
            case LOW -> 1;
        };
        for (String token : List.of(request.targetProgram(), request.careerInterest(), request.qualificationLevel(), profile.getInterests(), profile.getSkills(), profile.getLocation())) {
            if (token != null && !token.isBlank() && text.contains(token.toLowerCase(Locale.ROOT))) {
                score += 2;
            }
        }
        return score;
    }
}
