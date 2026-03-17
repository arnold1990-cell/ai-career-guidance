package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class UniversityPageRetrievalService {

    private static final int MAX_REPOSITORY_SCAN = 2_000;

    private final CrawledUniversityPageRepository repository;

    public UniversityPageRetrievalService(CrawledUniversityPageRepository repository) {
        this.repository = repository;
    }

    public List<UniversityPageSummary> retrieveTopRelevantPages(StudentProfile profile,
                                                                UniversitySourcesAnalysisRequest request,
                                                                int limit) {
        int effectiveLimit = Math.max(1, limit);
        return repository.findByActiveTrueAndCrawlStatus(CrawlStatus.SUCCESS, PageRequest.of(0, MAX_REPOSITORY_SCAN)).stream()
                .map(page -> toSummary(page, relevanceScore(page, profile, request)))
                .sorted(Comparator.comparingInt(UniversityPageSummary::relevanceScore).reversed()
                        .thenComparing(UniversityPageSummary::universityName))
                .limit(effectiveLimit)
                .toList();
    }

    private UniversityPageSummary toSummary(CrawledUniversityPage page, int relevanceScore) {
        return new UniversityPageSummary(
                page.getSourceUrl(),
                page.getUniversityName(),
                page.getPageTitle(),
                page.getPageType(),
                page.getQualificationLevel(),
                page.getExtractedKeywords(),
                page.getSummaryExcerpt(),
                relevanceScore
        );
    }

    private int relevanceScore(CrawledUniversityPage page, StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        String haystack = (safe(page.getPageTitle()) + " " + safe(page.getSummaryExcerpt()) + " "
                + safe(page.getCleanedContent()) + " " + String.join(" ", page.getExtractedKeywords())).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : buildTokens(profile, request)) {
            if (haystack.contains(token)) {
                score += 3;
            }
        }
        if ("PROGRAMME_DETAIL".equalsIgnoreCase(page.getPageType())) {
            score += 4;
        }
        if ("QUALIFICATION_LIST".equalsIgnoreCase(page.getPageType())) {
            score += 2;
        }
        if (safe(request.qualificationLevel()).equalsIgnoreCase(safe(page.getQualificationLevel()))) {
            score += 3;
        }
        if (containsAny(haystack, "software", "computer", "systems", "engineering", "technology", "information", "it")
                && containsAny(joinTokens(profile, request), "software", "computer", "systems", "engineering", "technology", "information", "it")) {
            score += 4;
        }
        return score;
    }

    private List<String> buildTokens(StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        Set<String> tokens = new LinkedHashSet<>();
        addSplit(tokens, request.targetProgram());
        addSplit(tokens, request.careerInterest());
        addSplit(tokens, request.qualificationLevel());
        addSplit(tokens, profile.getInterests());
        addSplit(tokens, profile.getSkills());
        addSplit(tokens, profile.getQualificationLevel());
        return new ArrayList<>(tokens);
    }

    private String joinTokens(StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        return String.join(" ", buildTokens(profile, request));
    }

    private void addSplit(Set<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String token : value.toLowerCase(Locale.ROOT).split("[,\\s/]+")) {
            if (token.length() >= 3) {
                target.add(token);
            }
        }
    }

    private boolean containsAny(String source, String... tokens) {
        for (String token : tokens) {
            if (source.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
