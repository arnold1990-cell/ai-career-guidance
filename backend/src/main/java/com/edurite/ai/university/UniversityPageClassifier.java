package com.edurite.ai.university;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UniversityPageClassifier {

    private static final List<String> PROGRAMME_DETAIL_HINTS = List.of(
            "programme overview", "program overview", "curriculum", "module list", "entry requirements",
            "career opportunities", "duration", "nqf level"
    );

    private static final List<String> FILTERED_PROGRAMME_LIST_HINTS = List.of(
            "filter programmes", "search programmes", "browse programmes", "programme finder", "program finder"
    );

    private static final List<String> QUALIFICATION_LIST_HINTS = List.of(
            "all qualifications", "undergraduate qualifications", "qualifications",
            "list of qualifications", "available qualifications"
    );

    private static final List<String> FEES_FUNDING_HINTS = List.of(
            "tuition fees", "financial aid", "funding", "bursary", "cost of study", "study fees"
    );

    private static final List<String> ADMISSIONS_HINTS = List.of(
            "apply for admission", "admission requirements", "how to apply", "admissions", "application dates"
    );

    private static final List<String> ACADEMIC_KEYWORDS = List.of(
            "computer science", "information systems", "accounting", "economics", "engineering", "software",
            "technology", "mathematics", "faculty", "college", "campus", "diploma", "degree", "bachelor"
    );

    public UniversityPageType classify(String title, String text) {
        String content = normalize(title) + "\n" + normalize(text);
        // Important: specific academic page types are checked before broad admissions wording.
        // This prevents admissions phrases from swallowing qualification/programme pages.
        if (containsAny(content, PROGRAMME_DETAIL_HINTS)) {
            return UniversityPageType.PROGRAMME_DETAIL;
        }
        if (containsAny(content, FILTERED_PROGRAMME_LIST_HINTS)) {
            return UniversityPageType.FILTERED_PROGRAMME_LIST;
        }
        if (containsAny(content, QUALIFICATION_LIST_HINTS)) {
            return UniversityPageType.QUALIFICATION_LIST;
        }
        if (containsAny(content, FEES_FUNDING_HINTS)) {
            return UniversityPageType.FEES_FUNDING;
        }
        if (containsAny(content, ADMISSIONS_HINTS)) {
            return UniversityPageType.ADMISSIONS_OVERVIEW;
        }
        return UniversityPageType.UNKNOWN;
    }

    public Set<String> extractKeywords(String title, String text) {
        String content = normalize(title) + "\n" + normalize(text);
        Set<String> extracted = new LinkedHashSet<>();
        for (String keyword : ACADEMIC_KEYWORDS) {
            if (content.contains(keyword)) {
                extracted.add(keyword);
            }
        }
        return extracted;
    }

    private boolean containsAny(String content, List<String> terms) {
        return terms.stream().anyMatch(content::contains);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
