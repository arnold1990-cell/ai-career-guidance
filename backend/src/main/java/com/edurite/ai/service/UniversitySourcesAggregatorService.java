package com.edurite.ai.service;

import com.edurite.ai.dto.FetchedUniversityPage;
import com.edurite.student.entity.StudentProfile;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourcesAggregatorService {

    private static final int MAX_COMBINED_TEXT = 12000;

    public AggregatedUniversityContext aggregate(List<FetchedUniversityPage> pages,
                                                 StudentProfile profile,
                                                 String targetProgram,
                                                 String careerInterest,
                                                 String qualificationLevel) {
        List<FetchedUniversityPage> successful = pages.stream().filter(FetchedUniversityPage::success).toList();

        String profileHints = (safe(profile.getInterests()) + " " + safe(profile.getSkills()) + " " + safe(targetProgram) + " "
                + safe(careerInterest) + " " + safe(qualificationLevel)).toLowerCase(Locale.ROOT);

        List<FetchedUniversityPage> ranked = successful.stream()
                .sorted(Comparator.comparingInt((FetchedUniversityPage p) -> relevanceScore(p, profileHints)).reversed())
                .toList();

        StringBuilder combined = new StringBuilder();
        Set<String> keywords = new LinkedHashSet<>();
        Set<String> warnings = new LinkedHashSet<>();

        for (FetchedUniversityPage page : ranked) {
            keywords.addAll(page.extractedKeywords());
            if (page.pageType().name().contains("LIST") || page.pageType().name().contains("OVERVIEW")) {
                warnings.add("Some pages are general listings and may not include programme-specific requirements.");
            }
            appendIfRoom(combined, "[Source: " + page.sourceUrl() + "] " + page.cleanedText());
            if (combined.length() >= MAX_COMBINED_TEXT) {
                break;
            }
        }

        return new AggregatedUniversityContext(
                combined.toString(),
                List.copyOf(keywords),
                List.copyOf(warnings),
                successful.stream().map(FetchedUniversityPage::sourceUrl).toList(),
                pages.stream().filter(p -> !p.success()).map(FetchedUniversityPage::sourceUrl).collect(Collectors.toList())
        );
    }

    private int relevanceScore(FetchedUniversityPage page, String hints) {
        int score = 0;
        String text = (safe(page.pageTitle()) + " " + safe(page.cleanedText()) + " " + String.join(" ", page.extractedKeywords())).toLowerCase(Locale.ROOT);
        if (hints.contains("software") || hints.contains("computer") || hints.contains("it")) {
            if (text.contains("computer") || text.contains("software") || text.contains("information systems")) {
                score += 4;
            }
        }
        if (hints.contains("grade 12") || hints.contains("undergraduate")) {
            if (text.contains("undergraduate")) {
                score += 3;
            }
        }
        score += Math.min(page.extractedKeywords().size(), 5);
        return score;
    }

    private void appendIfRoom(StringBuilder builder, String nextChunk) {
        if (builder.length() >= MAX_COMBINED_TEXT) {
            return;
        }
        int remaining = MAX_COMBINED_TEXT - builder.length();
        String normalized = nextChunk == null ? "" : nextChunk.trim();
        String chunk = normalized.length() <= remaining ? normalized : normalized.substring(0, remaining);
        if (!chunk.isBlank()) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(chunk);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record AggregatedUniversityContext(
            String mergedAcademicContext,
            List<String> extractedKeywords,
            List<String> warnings,
            List<String> successfulUrls,
            List<String> failedUrls
    ) {
    }
}
