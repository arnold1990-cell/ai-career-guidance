package com.edurite.ai.service;

import com.edurite.ai.dto.UniversityPageType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class UniversityPageClassifier {

    private static final List<String> PROGRAMME_HINTS = List.of(
            "computer science", "information systems", "accounting", "economics", "engineering",
            "software", "data science", "education", "law", "nursing", "business", "finance"
    );

    private static final Pattern FACULTY_PATTERN = Pattern.compile("\\b(faculty|college|school|campus)\\s+of\\s+([A-Za-z &-]{3,40})", Pattern.CASE_INSENSITIVE);
    private static final Pattern REQUIREMENT_PATTERN = Pattern.compile("\\b(requirements?|aps|minimum|admission|grade 12|mathematics|english)\\b", Pattern.CASE_INSENSITIVE);

    public UniversityPageType classify(String title, String content) {
        String merged = (safe(title) + " " + safe(content)).toLowerCase(Locale.ROOT);
        if (containsAny(merged, "fees", "funding", "bursary", "financial aid")) {
            return UniversityPageType.FEES_FUNDING;
        }
        if (containsAny(merged, "admission", "apply", "application")) {
            return UniversityPageType.ADMISSIONS_OVERVIEW;
        }
        if (containsAny(merged, "qualification", "programmes", "programs", "course finder", "undergraduate")) {
            return UniversityPageType.QUALIFICATION_LIST;
        }
        if (containsAny(merged, "programme details", "curriculum", "module")) {
            return UniversityPageType.PROGRAMME_DETAIL;
        }
        return UniversityPageType.UNKNOWN;
    }

    public List<String> extractKeywords(String title, String content) {
        String merged = (safe(title) + " " + safe(content)).toLowerCase(Locale.ROOT);
        Set<String> keywords = new LinkedHashSet<>();

        for (String hint : PROGRAMME_HINTS) {
            if (merged.contains(hint)) {
                keywords.add(toDisplay(hint));
            }
        }

        Matcher facultyMatcher = FACULTY_PATTERN.matcher(safe(content));
        while (facultyMatcher.find()) {
            keywords.add((facultyMatcher.group(1) + " of " + facultyMatcher.group(2)).trim());
        }

        if (REQUIREMENT_PATTERN.matcher(merged).find()) {
            keywords.add("Entry requirements");
        }
        return List.copyOf(keywords);
    }

    public List<String> extractNotes(String content) {
        String lower = safe(content).toLowerCase(Locale.ROOT);
        Set<String> notes = new LinkedHashSet<>();
        if (lower.contains("undergraduate")) {
            notes.add("Mentions undergraduate pathways");
        }
        if (lower.contains("postgraduate")) {
            notes.add("Mentions postgraduate pathways");
        }
        if (REQUIREMENT_PATTERN.matcher(lower).find()) {
            notes.add("Contains admission requirement hints");
        }
        return List.copyOf(notes);
    }

    private boolean containsAny(String input, String... values) {
        for (String value : values) {
            if (input.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String toDisplay(String keyword) {
        return switch (keyword) {
            case "computer science" -> "Computer Science";
            case "information systems" -> "Information Systems";
            case "data science" -> "Data Science";
            default -> keyword.substring(0, 1).toUpperCase(Locale.ROOT) + keyword.substring(1);
        };
    }
}
