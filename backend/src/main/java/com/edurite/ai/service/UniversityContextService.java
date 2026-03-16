package com.edurite.ai.service;

import com.edurite.ai.university.CrawlStatus;
import com.edurite.ai.university.CrawledUniversityPage;
import com.edurite.ai.university.CrawledUniversityPageRepository;
import com.edurite.institution.entity.Institution;
import com.edurite.institution.repository.InstitutionRepository;
import com.edurite.student.entity.StudentProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UniversityContextService {

    private final InstitutionRepository institutionRepository;
    private final CrawledUniversityPageRepository crawledUniversityPageRepository;

    public UniversityContextService(InstitutionRepository institutionRepository,
                                    CrawledUniversityPageRepository crawledUniversityPageRepository) {
        this.institutionRepository = institutionRepository;
        this.crawledUniversityPageRepository = crawledUniversityPageRepository;
    }

    public UniversityContextResult buildContext(StudentProfile profile, String question, int limit) {
        List<Institution> institutions = institutionRepository.findByActiveTrueOrderByFeaturedDescNameAsc();
        List<String> tokens = buildTokens(profile, question);

        List<Institution> matchedInstitutions = institutions.stream()
                .filter(institution -> scoreInstitution(institution, tokens) > 0)
                .limit(Math.max(1, limit))
                .toList();

        Set<String> institutionNames = matchedInstitutions.stream().map(Institution::getName).collect(java.util.stream.Collectors.toSet());
        Map<String, List<CrawledUniversityPage>> pagesByUniversity = crawledUniversityPageRepository
                .findByActiveTrueAndCrawlStatusAndUniversityNameIn(CrawlStatus.SUCCESS, institutionNames)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(CrawledUniversityPage::getUniversityName));

        List<MatchedUniversity> matched = matchedInstitutions.stream()
                .map(institution -> toMatchedUniversity(institution, pagesByUniversity.getOrDefault(institution.getName(), List.of()), tokens))
                .toList();

        return new UniversityContextResult(matched, toPromptContext(matched));
    }

    private MatchedUniversity toMatchedUniversity(Institution institution,
                                                  List<CrawledUniversityPage> pages,
                                                  List<String> tokens) {
        LinkedHashSet<String> programmes = new LinkedHashSet<>();
        LinkedHashSet<String> requirements = new LinkedHashSet<>();

        for (CrawledUniversityPage page : pages) {
            if (page.getExtractedKeywords() != null) {
                page.getExtractedKeywords().stream()
                        .filter(keyword -> shouldKeepKeyword(keyword, tokens))
                        .limit(8)
                        .forEach(programmes::add);
            }
            String excerpt = page.getSummaryExcerpt();
            if (excerpt != null && !excerpt.isBlank() && looksLikeRequirement(page, excerpt)) {
                requirements.add(excerpt);
            }
        }

        List<String> programmeList = new ArrayList<>(programmes).stream().limit(6).toList();
        List<String> requirementList = new ArrayList<>(requirements).stream().limit(4).toList();

        return new MatchedUniversity(
                institution.getName(),
                institution.getWebsite(),
                firstNonBlank(institution.getLocation(), institution.getCity(), institution.getProvince(), institution.getCountry(), "South Africa"),
                institution.getCategory(),
                programmeList,
                requirementList,
                "internal_university_module"
        );
    }

    private String toPromptContext(List<MatchedUniversity> matchedUniversities) {
        if (matchedUniversities.isEmpty()) {
            return "No exact institution-level matches were found in the internal universities module.";
        }

        return matchedUniversities.stream()
                .map(item -> "University: " + item.name()
                        + "\nWebsite: " + safe(item.officialWebsite())
                        + "\nLocation: " + safe(item.location())
                        + "\nProgrammes/keywords: " + String.join(", ", item.programmes())
                        + "\nEntry requirements: " + String.join(" | ", item.entryRequirements())
                        + "\nSource: " + item.source())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    private List<String> buildTokens(StudentProfile profile, String question) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        addTokens(tokens, question);
        addTokens(tokens, profile.getInterests());
        addTokens(tokens, profile.getSkills());
        addTokens(tokens, profile.getQualificationLevel());
        addTokens(tokens, profile.getCareerGoals());
        addTokens(tokens, profile.getLocation());
        return new ArrayList<>(tokens);
    }

    private void addTokens(Set<String> target, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String token : value.toLowerCase(Locale.ROOT).split("[,\\s/\\-]+")) {
            if (token.length() >= 3) {
                target.add(token);
            }
        }
    }

    private int scoreInstitution(Institution institution, List<String> tokens) {
        String haystack = (safe(institution.getName()) + " " + safe(institution.getCategory()) + " " + safe(institution.getLocation())
                + " " + safe(institution.getProvince()) + " " + safe(institution.getCity())).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : tokens) {
            if (haystack.contains(token)) {
                score += 2;
            }
        }
        if (Boolean.TRUE.equals(institution.getFeatured())) {
            score += 1;
        }
        return score;
    }

    private boolean shouldKeepKeyword(String keyword, List<String> tokens) {
        String normalized = safe(keyword).toLowerCase(Locale.ROOT);
        if (normalized.length() < 4) {
            return false;
        }
        if (tokens.isEmpty()) {
            return true;
        }
        return tokens.stream().anyMatch(normalized::contains) || tokens.stream().anyMatch(token -> token.contains(normalized));
    }

    private boolean looksLikeRequirement(CrawledUniversityPage page, String excerpt) {
        String content = (safe(page.getPageTitle()) + " " + safe(page.getPageType()) + " " + safe(excerpt)).toLowerCase(Locale.ROOT);
        return content.contains("require") || content.contains("admission") || content.contains("grade")
                || content.contains("subject") || content.contains("pass");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    public record MatchedUniversity(
            String name,
            String officialWebsite,
            String location,
            String category,
            List<String> programmes,
            List<String> entryRequirements,
            String source
    ) {
    }

    public record UniversityContextResult(List<MatchedUniversity> universities, String promptContext) {
        public List<String> universitySummaryLines() {
            return universities.stream()
                    .map(item -> item.name() + (item.officialWebsite() == null || item.officialWebsite().isBlank()
                            ? ""
                            : " - " + item.officialWebsite()))
                    .toList();
        }

        public List<String> entryRequirementLines() {
            return universities.stream()
                    .map(MatchedUniversity::entryRequirements)
                    .flatMap(Collection::stream)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .limit(8)
                    .toList();
        }

        public Map<String, List<String>> programmesByUniversity() {
            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (MatchedUniversity university : universities) {
                if (!university.programmes().isEmpty()) {
                    grouped.put(university.name(), university.programmes());
                }
            }
            return grouped;
        }
    }
}
