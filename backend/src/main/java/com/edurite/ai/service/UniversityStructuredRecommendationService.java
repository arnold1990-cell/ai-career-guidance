package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.student.entity.StudentProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class UniversityStructuredRecommendationService {

    private static final Map<String, List<String>> BROAD_INTEREST_EXPANSIONS = Map.of(
            "engineering", List.of("civil engineering", "mechanical engineering", "electrical engineering", "chemical engineering", "industrial engineering", "mechatronics", "computer engineering", "software engineering"),
            "technology", List.of("information technology", "computer science", "software engineering", "data science"),
            "business", List.of("accounting", "economics", "business management", "information systems")
    );

    private static final Map<String, List<String>> CAREER_PATHWAYS = Map.ofEntries(
            Map.entry("civil engineering", List.of("Civil Engineer")),
            Map.entry("mechanical engineering", List.of("Mechanical Engineer")),
            Map.entry("electrical engineering", List.of("Electrical Engineer")),
            Map.entry("chemical engineering", List.of("Chemical Engineer")),
            Map.entry("industrial engineering", List.of("Industrial Engineer")),
            Map.entry("mechatronics", List.of("Mechatronics Engineer", "Automation Engineer")),
            Map.entry("computer engineering", List.of("Computer Engineer", "Systems Engineer")),
            Map.entry("software engineering", List.of("Software Engineer", "Developer")),
            Map.entry("computer science", List.of("Software Developer", "Data Analyst")),
            Map.entry("information technology", List.of("IT Support Specialist", "Systems Analyst"))
    );

    private static final Pattern APS_PATTERN = Pattern.compile("\\bAPS\\b[^0-9]{0,12}(\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d{2})%\\s*(?:for|in)?\\s*([A-Za-z ]{3,30})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d)\\s*(year|years)", Pattern.CASE_INSENSITIVE);

    private final UniversitySourceRegistryService registryService;

    public UniversityStructuredRecommendationService(UniversitySourceRegistryService registryService) {
        this.registryService = registryService;
    }

    public UniversitySourcesAnalysisResponse buildResponse(UniversitySourcesAnalysisRequest request,
                                                           StudentProfile profile,
                                                           List<String> requestedUrls,
                                                           List<UniversitySourcePageResult> fetchedPages,
                                                           UniversitySourcesAnalysisResponse.AnalysisDiagnostics diagnostics,
                                                           String status,
                                                           String mode,
                                                           String message) {
        List<ExtractedProgramme> programmes = extractProgrammes(request, profile, fetchedPages);
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> recommendedProgrammes = programmes.stream()
                .limit(request.safeMaxRecommendations())
                .map(this::toProgramme)
                .toList();
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> careers = buildCareerRecommendations(programmes, request)
                .stream().limit(request.safeMaxRecommendations()).toList();
        List<String> universities = recommendedProgrammes.stream().map(UniversitySourcesAnalysisResponse.RecommendedProgramme::university).distinct().toList();
        List<String> minimumRequirements = programmes.stream().flatMap(programme -> programme.requirements.stream()).distinct().limit(8).toList();
        List<String> nextSteps = new ArrayList<>(List.of(
                "Review the official programme and admissions pages for your shortlisted institutions.",
                "Compare your school subjects against each programme's minimum requirements.",
                "Prepare supporting documents before application deadlines close."
        ));
        if (recommendedProgrammes.isEmpty()) {
            nextSteps.add("Broaden your search across related study areas and verify admissions requirements on official sites.");
        }
        List<String> warnings = diagnostics.technicalFailures() > 0 || diagnostics.timeouts() > 0
                ? List.of("Some institutions could not be analysed fully, so these results are partial.")
                : List.of();

        return new UniversitySourcesAnalysisResponse(
                status,
                false,
                true,
                mode,
                message,
                recommendedProgrammes.isEmpty() ? "NO_USEFUL_PROGRAMME_DATA" : "REGISTRY_GROUNDED",
                requestedUrls.isEmpty() ? 0 : (int) Math.round((diagnostics.usableSources() * 100.0) / requestedUrls.size()),
                warnings.isEmpty() ? null : warnings.get(0),
                requestedUrls,
                requestedUrls,
                fetchedPages.stream().filter(UniversitySourcePageResult::success).map(UniversitySourcePageResult::sourceUrl).toList(),
                fetchedPages.stream().filter(page -> !page.success()).map(UniversitySourcePageResult::sourceUrl).toList(),
                diagnostics.usableSources(),
                recommendedProgrammes.isEmpty()
                        ? "We checked curated official South African institution sources but did not find strong verified matches for this profile yet."
                        : "Recommendations were assembled from curated official South African institution sources.",
                List.of("Use official institution pages to confirm final admissions requirements and deadlines."),
                careers,
                recommendedProgrammes,
                List.of(),
                universities,
                minimumRequirements,
                minimumRequirements,
                programmes.stream().flatMap(programme -> programme.skillGaps.stream()).distinct().toList(),
                nextSteps,
                warnings,
                score(programmes, diagnostics),
                "registry-driven",
                recommendedProgrammes.isEmpty()
                        ? "The score is low because official sources did not provide enough verified programme matches for this profile."
                        : "The score reflects how closely your interests matched verified programme evidence from official institution pages.",
                buildSignals(request, profile, programmes, diagnostics),
                buildLimitations(programmes, diagnostics),
                List.of(),
                null,
                diagnostics
        );
    }

    private List<ExtractedProgramme> extractProgrammes(UniversitySourcesAnalysisRequest request,
                                                       StudentProfile profile,
                                                       List<UniversitySourcePageResult> pages) {
        List<String> interestTokens = expandedInterestTokens(request, profile);
        List<ExtractedProgramme> results = new ArrayList<>();
        for (UniversitySourcePageResult page : pages) {
            if (!page.success() || (page.pageType() == UniversityPageType.UNKNOWN)) {
                continue;
            }
            String combined = normalize(page.pageTitle() + " " + String.join(" ", page.headings()) + " " + page.cleanedText());
            int relevance = interestTokens.stream().mapToInt(token -> combined.contains(token) ? 4 : 0).sum();
            if (relevance == 0 && !combined.contains("programme") && !combined.contains("course") && !combined.contains("degree") && !combined.contains("diploma")) {
                continue;
            }
            String university = registryService.inferInstitutionName(page.sourceUrl());
            String programmeName = inferProgrammeName(page, interestTokens);
            List<String> requirements = extractRequirements(page.cleanedText());
            List<String> skillGaps = inferSkillGaps(request, requirements);
            results.add(new ExtractedProgramme(programmeName, university, requirements, buildNotes(page), skillGaps, relevance));
        }
        Map<String, ExtractedProgramme> deduped = new LinkedHashMap<>();
        for (ExtractedProgramme programme : results) {
            deduped.putIfAbsent(programme.name + "|" + programme.university, programme);
        }
        return deduped.values().stream().sorted((a, b) -> Integer.compare(b.relevance, a.relevance)).toList();
    }

    private String inferProgrammeName(UniversitySourcePageResult page, List<String> interestTokens) {
        String heading = page.headings().isEmpty() ? page.pageTitle() : page.headings().get(0);
        String normalizedHeading = normalize(heading);
        for (String token : interestTokens) {
            if (normalizedHeading.contains(token)) {
                return toTitleCase(token);
            }
        }
        if (normalizedHeading.contains("bachelor of")) {
            return toTitleCase(heading);
        }
        if (normalizedHeading.contains("diploma")) {
            return toTitleCase(heading);
        }
        return toTitleCase(heading.isBlank() ? page.pageTitle() : heading);
    }

    private List<String> extractRequirements(String text) {
        Set<String> requirements = new LinkedHashSet<>();
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ");
        Matcher aps = APS_PATTERN.matcher(normalized);
        if (aps.find()) {
            requirements.add("APS of at least " + aps.group(1) + " if confirmed on the official page.");
        }
        Matcher duration = DURATION_PATTERN.matcher(normalized);
        if (duration.find()) {
            requirements.add("Typical duration: " + duration.group(1) + " " + duration.group(2).toLowerCase(Locale.ROOT) + ".");
        }
        for (String subject : List.of("mathematics", "english", "physical sciences", "life sciences", "accounting")) {
            if (normalize(normalized).contains(subject)) {
                requirements.add(toTitleCase(subject) + " is referenced in the official source.");
            }
        }
        Matcher percent = PERCENT_PATTERN.matcher(normalized);
        if (percent.find()) {
            requirements.add(percent.group(2).trim() + " around " + percent.group(1) + "% if confirmed on the official source.");
        }
        if (requirements.isEmpty()) {
            requirements.add("See the official admissions page for the latest subject and APS requirements.");
        }
        return new ArrayList<>(requirements);
    }

    private List<String> inferSkillGaps(UniversitySourcesAnalysisRequest request, List<String> requirements) {
        List<String> gaps = new ArrayList<>();
        String normalizedInterest = normalize(request.careerInterest());
        if (normalizedInterest.contains("engineering") && requirements.stream().noneMatch(item -> normalize(item).contains("mathematics"))) {
            gaps.add("Confirm whether Mathematics and Physical Sciences are required for engineering pathways.");
        }
        if (requirements.stream().anyMatch(item -> normalize(item).contains("english"))) {
            gaps.add("Keep English performance strong for competitive admissions.");
        }
        return gaps.stream().distinct().toList();
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedCareer> buildCareerRecommendations(List<ExtractedProgramme> programmes,
                                                                                                 UniversitySourcesAnalysisRequest request) {
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> careers = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ExtractedProgramme programme : programmes) {
            String normalizedProgramme = normalize(programme.name);
            for (Map.Entry<String, List<String>> entry : CAREER_PATHWAYS.entrySet()) {
                if (!normalizedProgramme.contains(entry.getKey())) {
                    continue;
                }
                for (String career : entry.getValue()) {
                    if (seen.add(career)) {
                        careers.add(new UniversitySourcesAnalysisResponse.RecommendedCareer(
                                career,
                                "Recommended because official institution sources show a relevant programme pathway.",
                                programme.requirements,
                                List.of(programme.name)
                        ));
                    }
                }
            }
        }
        if (careers.isEmpty() && normalize(request.careerInterest()).contains("engineering")) {
            for (String area : BROAD_INTEREST_EXPANSIONS.get("engineering")) {
                String career = toTitleCase(area.replace(" engineering", " Engineer"));
                if (seen.add(career)) {
                    careers.add(new UniversitySourcesAnalysisResponse.RecommendedCareer(
                            career,
                            "Added as a broad engineering pathway so the guidance does not overfit to a narrow niche.",
                            List.of("Verify university-specific Mathematics, English, and Physical Sciences requirements."),
                            List.of(toTitleCase(area))
                    ));
                }
            }
        }
        return careers;
    }

    private UniversitySourcesAnalysisResponse.RecommendedProgramme toProgramme(ExtractedProgramme programme) {
        return new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                programme.name,
                programme.university,
                programme.requirements,
                programme.notes
        );
    }

    private int score(List<ExtractedProgramme> programmes, UniversitySourcesAnalysisResponse.AnalysisDiagnostics diagnostics) {
        int base = Math.min(85, programmes.size() * 18 + diagnostics.usableSources() * 5);
        if (diagnostics.technicalFailures() > 0 || diagnostics.timeouts() > 0) {
            base -= 10;
        }
        return Math.max(0, Math.min(100, base));
    }

    private List<String> buildSignals(UniversitySourcesAnalysisRequest request,
                                      StudentProfile profile,
                                      List<ExtractedProgramme> programmes,
                                      UniversitySourcesAnalysisResponse.AnalysisDiagnostics diagnostics) {
        List<String> signals = new ArrayList<>();
        if (request.targetProgram() != null && !request.targetProgram().isBlank()) {
            signals.add("Target programme: " + request.targetProgram());
        }
        if (request.careerInterest() != null && !request.careerInterest().isBlank()) {
            signals.add("Career interest: " + request.careerInterest());
        }
        if (profile.getInterests() != null && !profile.getInterests().isBlank()) {
            signals.add("Profile interests: " + profile.getInterests());
        }
        signals.add("Verified official sources: " + diagnostics.usableSources());
        signals.add("Matched programmes: " + programmes.size());
        return signals;
    }

    private List<String> buildLimitations(List<ExtractedProgramme> programmes,
                                          UniversitySourcesAnalysisResponse.AnalysisDiagnostics diagnostics) {
        List<String> limitations = new ArrayList<>();
        if (diagnostics.technicalFailures() > 0) {
            limitations.add("Some official institution pages could not be fetched.");
        }
        if (diagnostics.timeouts() > 0) {
            limitations.add("Some official pages timed out before they could be analysed.");
        }
        if (programmes.isEmpty()) {
            limitations.add("No sufficiently strong verified programme matches were extracted from the fetched pages.");
        }
        return limitations;
    }

    private List<String> expandedInterestTokens(UniversitySourcesAnalysisRequest request, StudentProfile profile) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : List.of(request.targetProgram(), request.careerInterest(), profile.getInterests(), profile.getSkills())) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String normalized = normalize(raw);
            tokens.add(normalized);
            Arrays.stream(normalized.split("[,/]+|\\s{2,}"))
                    .map(String::trim)
                    .filter(token -> !token.isBlank())
                    .forEach(tokens::add);
            for (Map.Entry<String, List<String>> entry : BROAD_INTEREST_EXPANSIONS.entrySet()) {
                if (normalized.contains(entry.getKey())) {
                    tokens.addAll(entry.getValue());
                }
            }
        }
        return tokens.stream().filter(token -> !token.isBlank()).toList();
    }

    private String buildNotes(UniversitySourcePageResult page) {
        return "Verified from official source page: " + page.sourceUrl();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private String toTitleCase(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        StringBuilder builder = new StringBuilder();
        for (String token : normalized.split(" ")) {
            if (token.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
        }
        return builder.toString();
    }

    private static final class ExtractedProgramme {
        private final String name;
        private final String university;
        private final List<String> requirements;
        private final String notes;
        private final List<String> skillGaps;
        private final int relevance;

        private ExtractedProgramme(String name, String university, List<String> requirements, String notes, List<String> skillGaps, int relevance) {
            this.name = name;
            this.university = university;
            this.requirements = requirements;
            this.notes = notes;
            this.skillGaps = skillGaps;
            this.relevance = relevance;
        }
    }
}
