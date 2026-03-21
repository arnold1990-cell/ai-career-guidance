package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourcesAggregatorService {

    private static final int MAX_COMBINED_CONTEXT_CHARS = 20_000;
    private static final int MAX_PAGES_PER_UNIVERSITY = 3;

    public String buildCombinedContext(List<UniversitySourcePageResult> pages, StudentProfile profile,
                                      UniversitySourcesAnalysisRequest request) {
        List<UniversitySourcePageResult> successful = pages.stream()
                .filter(UniversitySourcePageResult::success)
                .sorted(Comparator.comparingInt((UniversitySourcePageResult p) -> relevanceScore(p, profile, request)).reversed())
                .toList();

        Map<String, Deque<UniversitySourcePageResult>> grouped = new LinkedHashMap<>();
        for (UniversitySourcePageResult page : successful) {
            String universityKey = inferUniversity(page.sourceUrl());
            Deque<UniversitySourcePageResult> bucket = grouped.computeIfAbsent(universityKey, ignored -> new ArrayDeque<>());
            if (bucket.size() < MAX_PAGES_PER_UNIVERSITY) {
                bucket.add(page);
            }
        }

        StringBuilder builder = new StringBuilder();
        boolean added;
        do {
            added = false;
            for (Map.Entry<String, Deque<UniversitySourcePageResult>> entry : grouped.entrySet()) {
                UniversitySourcePageResult page = entry.getValue().pollFirst();
                if (page == null) {
                    continue;
                }
                String block = "University: " + entry.getKey() + "\n"
                        + "Source URL: " + page.sourceUrl() + "\n"
                        + "Title: " + page.pageTitle() + "\n"
                        + "Type: " + page.pageType() + "\n"
                        + "Headings: " + String.join(" | ", page.headings()) + "\n"
                        + "Keywords: " + String.join(", ", page.extractedKeywords()) + "\n"
                        + page.cleanedText();
                if (builder.length() + block.length() + 2 > MAX_COMBINED_CONTEXT_CHARS) {
                    return builder.toString().trim();
                }
                builder.append(block).append("\n\n");
                added = true;
            }
        } while (added);
        return builder.toString().trim();
    }

    private int relevanceScore(UniversitySourcePageResult page, StudentProfile profile,
                               UniversitySourcesAnalysisRequest request) {
        int score = 0;
        String haystack = (page.pageTitle() + " " + page.cleanedText() + " " + String.join(" ", page.extractedKeywords()) + " " + String.join(" ", page.headings()))
                .toLowerCase(Locale.ROOT);
        for (String token : buildTokens(profile, request)) {
            if (!token.isBlank() && haystack.contains(token)) {
                score += 3;
            }
        }
        if (page.pageType() == UniversityPageType.PROGRAMME_DETAIL) {
            score += 5;
        }
        if (page.pageType() == UniversityPageType.QUALIFICATION_LIST) {
            score += 4;
        }
        if (page.pageType() == UniversityPageType.ADMISSIONS_OVERVIEW) {
            score += 3;
        }
        if (page.pageType() == UniversityPageType.FEES_FUNDING) {
            score += 2;
        }
        if (!page.headings().isEmpty()) {
            score += 1;
        }
        return score;
    }

    private List<String> buildTokens(StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        List<String> tokens = new ArrayList<>();
        addSplit(tokens, request.targetProgram());
        addSplit(tokens, request.careerInterest());
        addSplit(tokens, request.qualificationLevel());
        addSplit(tokens, profile.getInterests());
        addSplit(tokens, profile.getSkills());
        addSplit(tokens, profile.getQualificationLevel());
        return tokens;
    }

    private void addSplit(List<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String token : value.toLowerCase(Locale.ROOT).split("[,\\s/]+")) {
            if (token.length() >= 3) {
                target.add(token);
            }
        }
    }

    private String inferUniversity(String sourceUrl) {
        try {
            URI uri = URI.create(sourceUrl);
            if (uri.getHost() == null) {
                return "Official source";
            }
            String host = uri.getHost().replaceFirst("^www\\.", "");
            return host;
        } catch (RuntimeException ex) {
            return "Official source";
        }
    }
}
