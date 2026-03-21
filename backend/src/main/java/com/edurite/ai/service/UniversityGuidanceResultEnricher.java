package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UniversityGuidanceResultEnricher {

    public UniversitySourcesAnalysisResponse enrich(UniversitySourcesAnalysisResponse response,
                                                    UniversitySourcesAnalysisRequest request,
                                                    StudentProfile profile,
                                                    List<String> requestedUrls,
                                                    List<UniversitySourcePageResult> fetchedPages) {
        List<UniversitySourcePageResult> safePages = fetchedPages == null ? List.of() : fetchedPages;
        List<String> safeRequestedUrls = requestedUrls == null ? List.of() : requestedUrls;
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes = enrichProgrammes(response, request, profile, safePages);
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> careers = enrichCareers(response, request, profile, programmes, safePages);
        List<UniversitySourcesAnalysisResponse.SourceDiagnostic> sourceDiagnostics = buildDiagnostics(safeRequestedUrls, safePages);
        UniversitySourcesAnalysisResponse.SourceCoverage sourceCoverage = buildCoverage(safeRequestedUrls, safePages, programmes, sourceDiagnostics);
        String scoreReason = buildScoreReason(response, request, profile, programmes, safePages);
        List<String> scoreSignals = buildScoreSignals(request, profile, programmes, safePages);
        List<String> scoreLimitations = buildScoreLimitations(programmes, sourceDiagnostics);
        String summary = buildSummary(response, request, careers, programmes, sourceCoverage);

        return new UniversitySourcesAnalysisResponse(
                response.aiLive(),
                response.fallbackUsed(),
                response.status(),
                response.mode(),
                response.groundingStatus(),
                response.evidenceCoverage(),
                response.warningMessage(),
                response.requestedSources(),
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
                summary,
                dedupeStrings(response.inferredGuidance()),
                careers,
                programmes,
                response.bursarySuggestions(),
                dedupeStrings(response.recommendedUniversities()),
                dedupeStrings(response.minimumRequirements()),
                dedupeStrings(response.keyRequirements()),
                dedupeStrings(response.skillGaps()),
                dedupeStrings(response.recommendedNextSteps()),
                dedupeStrings(response.warnings()),
                response.suitabilityScore(),
                response.rawModelUsed(),
                scoreReason,
                scoreSignals,
                scoreLimitations,
                sourceDiagnostics,
                sourceCoverage
        );
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedProgramme> enrichProgrammes(UniversitySourcesAnalysisResponse response,
                                                                                           UniversitySourcesAnalysisRequest request,
                                                                                           StudentProfile profile,
                                                                                           List<UniversitySourcePageResult> pages) {
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> base = response.recommendedProgrammes() == null ? List.of() : response.recommendedProgrammes();
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> result = new ArrayList<>();
        for (UniversitySourcesAnalysisResponse.RecommendedProgramme programme : base) {
            List<UniversitySourcePageResult> relevantPages = relevantProgrammePages(programme, pages);
            List<String> verifiedFacts = extractVerifiedFacts(programme.name(), programme.university(), relevantPages);
            List<String> missingData = buildMissingProgrammeData(programme, verifiedFacts, relevantPages);
            List<String> nextBestActions = buildProgrammeActions(programme, missingData);
            List<String> inferredInsights = buildProgrammeInsights(programme, request, profile, verifiedFacts);
            result.add(new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                    programme.name(),
                    programme.university(),
                    dedupeRequirements(programme.admissionRequirements()),
                    programme.notes(),
                    firstNonBlank(programme.recommendationReason(), buildProgrammeReason(programme, request, profile, verifiedFacts, relevantPages)),
                    firstNonBlank(programme.confidenceLevel(), confidenceLevel(verifiedFacts, missingData)),
                    verifiedFacts,
                    inferredInsights,
                    missingData,
                    resolveSourceStatus(verifiedFacts, missingData, relevantPages),
                    null,
                    nextBestActions
            ));
        }
        return sortProgrammes(result, request, profile);
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedCareer> enrichCareers(UniversitySourcesAnalysisResponse response,
                                                                                     UniversitySourcesAnalysisRequest request,
                                                                                     StudentProfile profile,
                                                                                     List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                                                                     List<UniversitySourcePageResult> pages) {
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> base = response.recommendedCareers() == null ? List.of() : response.recommendedCareers();
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> result = new ArrayList<>();
        for (UniversitySourcesAnalysisResponse.RecommendedCareer career : base) {
            List<String> verifiedFacts = extractCareerFacts(career, programmes, pages);
            List<String> missingData = verifiedFacts.isEmpty()
                    ? List.of("Official pathway requirements are limited in fetched sources.")
                    : List.of();
            result.add(new UniversitySourcesAnalysisResponse.RecommendedCareer(
                    career.name(),
                    career.reason(),
                    dedupeStrings(career.requirements()),
                    dedupeStrings(career.relatedProgrammes()),
                    firstNonBlank(career.recommendationReason(), buildCareerReason(career, request, profile, programmes)),
                    firstNonBlank(career.confidenceLevel(), confidenceLevel(verifiedFacts, missingData)),
                    verifiedFacts,
                    buildCareerInsights(career, request, profile, programmes),
                    missingData,
                    resolveSourceStatus(verifiedFacts, missingData, pages),
                    null,
                    buildCareerActions(career, programmes)
            ));
        }
        return sortCareers(result, request, profile);
    }

    private List<UniversitySourcesAnalysisResponse.SourceDiagnostic> buildDiagnostics(List<String> requestedUrls,
                                                                                      List<UniversitySourcePageResult> pages) {
        List<UniversitySourcesAnalysisResponse.SourceDiagnostic> diagnostics = new ArrayList<>();
        for (String requestedUrl : requestedUrls) {
            UniversitySourcePageResult page = pages.stream().filter(item -> requestedUrl.equals(item.sourceUrl())).findFirst().orElse(null);
            if (page == null) {
                diagnostics.add(new UniversitySourcesAnalysisResponse.SourceDiagnostic(requestedUrl, "FAILED", "Source was requested but no fetch result was recorded.", inferUniversity(requestedUrl), false));
                continue;
            }
            diagnostics.add(new UniversitySourcesAnalysisResponse.SourceDiagnostic(
                    page.sourceUrl(),
                    diagnosticStatus(page),
                    fallbackFailureReason(page),
                    inferUniversity(page.sourceUrl()),
                    page.success() && isProgrammeUsable(page)
            ));
        }
        return diagnostics;
    }

    private UniversitySourcesAnalysisResponse.SourceCoverage buildCoverage(List<String> requestedUrls,
                                                                          List<UniversitySourcePageResult> pages,
                                                                          List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                                                          List<UniversitySourcesAnalysisResponse.SourceDiagnostic> sourceDiagnostics) {
        int successCount = (int) sourceDiagnostics.stream().filter(item -> "SUCCESS".equalsIgnoreCase(item.fetchStatus())).count();
        int partialCount = (int) sourceDiagnostics.stream().filter(item -> "PARTIAL".equalsIgnoreCase(item.fetchStatus())).count();
        int failedCount = Math.max(0, requestedUrls.size() - successCount - partialCount);
        List<String> universities = dedupeStrings(programmes.stream().map(UniversitySourcesAnalysisResponse.RecommendedProgramme::university).collect(Collectors.toList()));
        if (universities.isEmpty()) {
            universities = dedupeStrings(sourceDiagnostics.stream()
                    .filter(UniversitySourcesAnalysisResponse.SourceDiagnostic::usableProgrammeData)
                    .map(UniversitySourcesAnalysisResponse.SourceDiagnostic::university)
                    .collect(Collectors.toList()));
        }
        return new UniversitySourcesAnalysisResponse.SourceCoverage(requestedUrls.size(), successCount, failedCount, partialCount, universities);
    }

    private List<String> extractVerifiedFacts(String programmeName, String university, List<UniversitySourcePageResult> pages) {
        Set<String> facts = new LinkedHashSet<>();
        String programmeNeedle = normalize(programmeName);
        String universityNeedle = normalize(university);
        for (UniversitySourcePageResult page : pages) {
            if (!page.success()) {
                continue;
            }
            String combined = normalize(page.pageTitle() + " " + page.cleanedText() + " " + String.join(" ", page.headings()));
            if ((!programmeNeedle.isBlank() && combined.contains(programmeNeedle))
                    || (!universityNeedle.isBlank() && combined.contains(universityNeedle))
                    || isProgrammeUsable(page)) {
                facts.add("Verified from " + inferUniversity(page.sourceUrl()) + ": " + summarizeFact(page));
            }
            if (facts.size() >= 3) {
                break;
            }
        }
        return new ArrayList<>(facts);
    }

    private List<String> extractCareerFacts(UniversitySourcesAnalysisResponse.RecommendedCareer career,
                                            List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                            List<UniversitySourcePageResult> pages) {
        Set<String> facts = new LinkedHashSet<>();
        for (UniversitySourcesAnalysisResponse.RecommendedProgramme programme : programmes) {
            if (programme.name() != null && career.relatedProgrammes() != null && career.relatedProgrammes().stream().anyMatch(item -> item.equalsIgnoreCase(programme.name()))) {
                facts.addAll(programme.verifiedFacts() == null ? List.of() : programme.verifiedFacts());
            }
        }
        if (facts.isEmpty()) {
            for (UniversitySourcePageResult page : pages) {
                if (page.success() && isProgrammeUsable(page)) {
                    facts.add("Verified from official source: " + summarizeFact(page));
                }
                if (facts.size() >= 2) {
                    break;
                }
            }
        }
        return new ArrayList<>(facts);
    }

    private List<String> buildMissingProgrammeData(UniversitySourcesAnalysisResponse.RecommendedProgramme programme,
                                                   List<String> verifiedFacts,
                                                   List<UniversitySourcePageResult> relevantPages) {
        List<String> missing = new ArrayList<>();
        String combinedRequirements = String.join(" ", programme.admissionRequirements() == null ? List.of() : programme.admissionRequirements()).toLowerCase(Locale.ROOT);
        String combinedFacts = String.join(" ", verifiedFacts).toLowerCase(Locale.ROOT);
        String combinedNotes = safe(programme.notes()).toLowerCase(Locale.ROOT);
        if (!containsAny(combinedRequirements + " " + combinedFacts + " " + combinedNotes, List.of("aps", "admission point score"))) {
            missing.add("APS not found in fetched sources.");
        }
        if (!containsAny(combinedRequirements + " " + combinedFacts + " " + combinedNotes, List.of("deadline", "closing date", "application date"))) {
            missing.add("Deadline missing from fetched sources.");
        }
        if (verifiedFacts.isEmpty()) {
            missing.add("Official page unreachable or lacked programme-specific details.");
        }
        if (!relevantPages.isEmpty() && relevantPages.stream().allMatch(page -> !page.success())) {
            missing.add("Source unavailable from official page.");
        }
        return dedupeStrings(missing);
    }

    private List<String> buildProgrammeActions(UniversitySourcesAnalysisResponse.RecommendedProgramme programme,
                                               List<String> missingData) {
        List<String> actions = new ArrayList<>();
        actions.add("Compare your subjects against the official programme requirements.");
        if (!missingData.isEmpty()) {
            actions.add("Open the official admissions page to verify missing APS, deadline, or subject details.");
        }
        if (programme.university() != null && !programme.university().isBlank()) {
            actions.add("Shortlist " + programme.university() + " for direct application tracking.");
        }
        return dedupeStrings(actions);
    }

    private List<String> buildCareerActions(UniversitySourcesAnalysisResponse.RecommendedCareer career,
                                            List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes) {
        List<String> actions = new ArrayList<>();
        actions.add("Review the linked programmes for " + career.name() + ".");
        if (career.requirements() != null && !career.requirements().isEmpty()) {
            actions.add("Strengthen the top requirement gaps before applying.");
        }
        if (!programmes.isEmpty()) {
            actions.add("Compare universities offering the strongest-fit related programmes.");
        }
        return dedupeStrings(actions);
    }

    private String buildProgrammeReason(UniversitySourcesAnalysisResponse.RecommendedProgramme programme,
                                        UniversitySourcesAnalysisRequest request,
                                        StudentProfile profile,
                                        List<String> verifiedFacts,
                                        List<UniversitySourcePageResult> relevantPages) {
        List<String> reasons = new ArrayList<>();
        if (containsIgnoreCase(programme.name(), request.targetProgram())) {
            reasons.add("matches your target programme");
        }
        if (containsIgnoreCase(programme.name(), request.careerInterest())) {
            reasons.add("aligns with your stated career interest");
        }
        if (containsIgnoreCase(profile.getInterests(), programme.name()) || containsIgnoreCase(profile.getSkills(), programme.name())) {
            reasons.add("connects with your profile interests or skills");
        }
        if (!verifiedFacts.isEmpty()) {
            reasons.add("has supporting official source evidence");
        }
        if (relevantPages.stream().map(this::inferUniversity).distinct().count() > 1) {
            reasons.add("appears across multiple usable official source pages");
        }
        return reasons.isEmpty() ? "Recommended as a practical next-fit programme based on your profile and available university evidence." :
                "Recommended because it " + String.join(", ", reasons) + ".";
    }

    private String buildCareerReason(UniversitySourcesAnalysisResponse.RecommendedCareer career,
                                     UniversitySourcesAnalysisRequest request,
                                     StudentProfile profile,
                                     List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes) {
        List<String> reasons = new ArrayList<>();
        if (containsIgnoreCase(career.name(), request.careerInterest())) {
            reasons.add("directly reflects your career interest");
        }
        if (containsIgnoreCase(profile.getInterests(), career.name()) || containsIgnoreCase(profile.getSkills(), career.name())) {
            reasons.add("fits your profile interests or skills");
        }
        if (career.relatedProgrammes() != null && !career.relatedProgrammes().isEmpty()) {
            reasons.add("links to available programme pathways");
        }
        long supportingUniversities = programmes.stream()
                .filter(programme -> career.relatedProgrammes() != null && career.relatedProgrammes().stream().anyMatch(item -> item.equalsIgnoreCase(programme.name())))
                .map(UniversitySourcesAnalysisResponse.RecommendedProgramme::university)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .count();
        if (supportingUniversities > 0) {
            reasons.add("is supported by " + supportingUniversities + " university option(s)");
        }
        return reasons.isEmpty() ? "Recommended as a realistic pathway given your current profile." : "Recommended because it " + String.join(", ", reasons) + ".";
    }

    private String buildScoreReason(UniversitySourcesAnalysisResponse response,
                                    UniversitySourcesAnalysisRequest request,
                                    StudentProfile profile,
                                    List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                    List<UniversitySourcePageResult> pages) {
        int score = response.suitabilityScore() == null ? 0 : response.suitabilityScore();
        long successCount = pages.stream().filter(UniversitySourcePageResult::success).count();
        long supportingUniversities = programmes.stream().map(UniversitySourcesAnalysisResponse.RecommendedProgramme::university).filter(value -> value != null && !value.isBlank()).distinct().count();
        return "Suitability score " + score + " was based on your requested programme and career goals, stored profile interests/skills, and "
                + successCount + " successfully fetched official source(s) across " + supportingUniversities + " university option(s). "
                + (programmes.isEmpty()
                ? "The score was reduced because few programme matches were verified from official pages."
                : "The score improved when programme matches aligned with your stated interests and official university evidence.");
    }

    private List<String> buildScoreSignals(UniversitySourcesAnalysisRequest request,
                                           StudentProfile profile,
                                           List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                           List<UniversitySourcePageResult> pages) {
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
        if (profile.getSkills() != null && !profile.getSkills().isBlank()) {
            signals.add("Profile skills: " + profile.getSkills());
        }
        if (!programmes.isEmpty()) {
            signals.add("Programme matches identified: " + programmes.size());
            signals.add("Universities contributing evidence: " + programmes.stream().map(UniversitySourcesAnalysisResponse.RecommendedProgramme::university).filter(value -> value != null && !value.isBlank()).distinct().count());
        }
        signals.add("Successful official sources: " + pages.stream().filter(UniversitySourcePageResult::success).count());
        return dedupeStrings(signals);
    }

    private List<String> buildScoreLimitations(List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                               List<UniversitySourcesAnalysisResponse.SourceDiagnostic> sourceDiagnostics) {
        List<String> limitations = new ArrayList<>();
        if (sourceDiagnostics.stream().anyMatch(item -> "FAILED".equalsIgnoreCase(item.fetchStatus()) || "TIMEOUT".equalsIgnoreCase(item.fetchStatus()))) {
            limitations.add("Some official pages failed or timed out, so source coverage is incomplete.");
        }
        if (programmes.stream().anyMatch(programme -> programme.missingData() != null && !programme.missingData().isEmpty())) {
            limitations.add("Missing APS, deadline, or subject detail reduced score certainty.");
        }
        if (limitations.isEmpty()) {
            limitations.add("No major data limitations were detected in the fetched sources.");
        }
        return limitations;
    }

    private String buildSummary(UniversitySourcesAnalysisResponse response,
                                UniversitySourcesAnalysisRequest request,
                                List<UniversitySourcesAnalysisResponse.RecommendedCareer> careers,
                                List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                UniversitySourcesAnalysisResponse.SourceCoverage coverage) {
        String current = safe(response.summary());
        boolean generic = current.isBlank() || current.length() < 40 || current.toLowerCase(Locale.ROOT).contains("based on your profile and current guidance context");
        if (!generic && coverage != null && coverage.successfulSourcesCount() != null && coverage.successfulSourcesCount() > 1) {
            return current;
        }
        String topCareer = careers.isEmpty() ? "" : safe(careers.get(0).name());
        String topProgramme = programmes.isEmpty() ? "" : safe(programmes.get(0).name()) + (programmes.get(0).university() == null ? "" : " at " + programmes.get(0).university());
        int successfulSources = coverage == null || coverage.successfulSourcesCount() == null ? 0 : coverage.successfulSourcesCount();
        return (request.careerInterest() == null || request.careerInterest().isBlank() ? "Your current profile" : "Your interest in " + request.careerInterest())
                + " is strongest for "
                + (topCareer.isBlank() ? "the leading guidance options" : topCareer)
                + ", with "
                + (topProgramme.isBlank() ? "programme pathways still needing more official verification" : topProgramme)
                + " standing out from " + successfulSources + " successful official source(s)."
                + (coverage != null && coverage.partialSourcesCount() != null && coverage.partialSourcesCount() > 0 ? " Some sources were only partially usable, so verify deadlines and APS details on official admissions pages." : "");
    }

    private List<String> buildProgrammeInsights(UniversitySourcesAnalysisResponse.RecommendedProgramme programme,
                                                UniversitySourcesAnalysisRequest request,
                                                StudentProfile profile,
                                                List<String> verifiedFacts) {
        List<String> insights = new ArrayList<>();
        if (containsIgnoreCase(programme.name(), request.careerInterest()) || containsIgnoreCase(programme.notes(), request.careerInterest())) {
            insights.add("AI inference: this programme keeps you close to your stated career direction.");
        }
        if (containsIgnoreCase(profile.getInterests(), programme.name()) || containsIgnoreCase(profile.getSkills(), programme.name())) {
            insights.add("AI inference: your stored interests or skills suggest you may adapt well to this programme.");
        }
        if (!verifiedFacts.isEmpty()) {
            insights.add("AI inference: verified programme evidence improves confidence in this recommendation.");
        }
        insights.addAll(dedupeStrings(java.util.Arrays.asList(programme.notes(), programme.recommendationReason())));
        return dedupeStrings(insights);
    }

    private List<String> buildCareerInsights(UniversitySourcesAnalysisResponse.RecommendedCareer career,
                                             UniversitySourcesAnalysisRequest request,
                                             StudentProfile profile,
                                             List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes) {
        List<String> insights = new ArrayList<>();
        if (containsIgnoreCase(career.name(), request.careerInterest())) {
            insights.add("AI inference: this career directly matches the pathway you asked EduRite to assess.");
        }
        if (containsIgnoreCase(profile.getInterests(), career.name()) || containsIgnoreCase(profile.getSkills(), career.name())) {
            insights.add("AI inference: your profile suggests a stronger-than-generic fit for this career.");
        }
        long linkedProgrammes = programmes.stream().filter(programme -> career.relatedProgrammes() != null && career.relatedProgrammes().stream().anyMatch(item -> item.equalsIgnoreCase(programme.name()))).count();
        if (linkedProgrammes > 0) {
            insights.add("AI inference: there are " + linkedProgrammes + " linked programme option(s) supporting this pathway.");
        }
        insights.add(career.reason());
        return dedupeStrings(insights);
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedProgramme> sortProgrammes(List<UniversitySourcesAnalysisResponse.RecommendedProgramme> programmes,
                                                                                         UniversitySourcesAnalysisRequest request,
                                                                                         StudentProfile profile) {
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> sorted = programmes.stream()
                .sorted(Comparator.comparingInt((UniversitySourcesAnalysisResponse.RecommendedProgramme programme) -> programmeRank(programme, request, profile)).reversed()
                        .thenComparing(UniversitySourcesAnalysisResponse.RecommendedProgramme::university, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(UniversitySourcesAnalysisResponse.RecommendedProgramme::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> ranked = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            UniversitySourcesAnalysisResponse.RecommendedProgramme programme = sorted.get(index);
            ranked.add(new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                    programme.name(),
                    programme.university(),
                    programme.admissionRequirements(),
                    programme.notes(),
                    programme.recommendationReason(),
                    programme.confidenceLevel(),
                    programme.verifiedFacts(),
                    programme.inferredInsights(),
                    programme.missingData(),
                    programme.sourceStatus(),
                    rankingCategory(index),
                    programme.nextBestActions()
            ));
        }
        return ranked;
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedCareer> sortCareers(List<UniversitySourcesAnalysisResponse.RecommendedCareer> careers,
                                                                                   UniversitySourcesAnalysisRequest request,
                                                                                   StudentProfile profile) {
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> sorted = careers.stream()
                .sorted(Comparator.comparingInt((UniversitySourcesAnalysisResponse.RecommendedCareer career) -> careerRank(career, request, profile)).reversed()
                        .thenComparing(UniversitySourcesAnalysisResponse.RecommendedCareer::name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> ranked = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            UniversitySourcesAnalysisResponse.RecommendedCareer career = sorted.get(index);
            ranked.add(new UniversitySourcesAnalysisResponse.RecommendedCareer(
                    career.name(),
                    career.reason(),
                    career.requirements(),
                    career.relatedProgrammes(),
                    career.recommendationReason(),
                    career.confidenceLevel(),
                    career.verifiedFacts(),
                    career.inferredInsights(),
                    career.missingData(),
                    career.sourceStatus(),
                    rankingCategory(index),
                    career.nextBestActions()
            ));
        }
        return ranked;
    }

    private int programmeRank(UniversitySourcesAnalysisResponse.RecommendedProgramme programme,
                              UniversitySourcesAnalysisRequest request,
                              StudentProfile profile) {
        int score = 0;
        if (containsIgnoreCase(programme.name(), request.targetProgram())) {
            score += 5;
        }
        if (containsIgnoreCase(programme.name(), request.careerInterest())) {
            score += 4;
        }
        if (containsIgnoreCase(profile.getInterests(), programme.name()) || containsIgnoreCase(profile.getSkills(), programme.name())) {
            score += 3;
        }
        score += programme.verifiedFacts() == null ? 0 : programme.verifiedFacts().size() * 2;
        score -= programme.missingData() == null ? 0 : programme.missingData().size();
        return score;
    }

    private int careerRank(UniversitySourcesAnalysisResponse.RecommendedCareer career,
                           UniversitySourcesAnalysisRequest request,
                           StudentProfile profile) {
        int score = 0;
        if (containsIgnoreCase(career.name(), request.careerInterest())) {
            score += 5;
        }
        if (containsIgnoreCase(profile.getInterests(), career.name()) || containsIgnoreCase(profile.getSkills(), career.name())) {
            score += 3;
        }
        score += career.verifiedFacts() == null ? 0 : career.verifiedFacts().size() * 2;
        score += career.relatedProgrammes() == null ? 0 : career.relatedProgrammes().size();
        return score;
    }

    private List<UniversitySourcePageResult> relevantProgrammePages(UniversitySourcesAnalysisResponse.RecommendedProgramme programme,
                                                                    List<UniversitySourcePageResult> pages) {
        String programmeNeedle = normalize(programme.name());
        String universityNeedle = normalize(programme.university());
        List<UniversitySourcePageResult> matched = pages.stream()
                .filter(page -> containsIgnoreCase(page.sourceUrl(), programme.university())
                        || containsIgnoreCase(page.pageTitle(), programme.name())
                        || containsIgnoreCase(page.cleanedText(), programme.name())
                        || containsIgnoreCase(String.join(" ", page.headings()), programme.name())
                        || (!universityNeedle.isBlank() && normalize(inferUniversity(page.sourceUrl())).contains(universityNeedle))
                        || (!programmeNeedle.isBlank() && normalize(String.join(" ", page.extractedKeywords())).contains(programmeNeedle)))
                .toList();
        return matched.isEmpty() ? pages : matched;
    }

    private List<String> dedupeRequirements(List<String> requirements) {
        List<String> safe = dedupeStrings(requirements);
        Set<String> normalized = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();
        for (String item : safe) {
            String canonical = normalize(item)
                    .replace("not found in fetched sources", "missing requirement data")
                    .replace("verify exact programme requirements from official university programme pages.", "verify on official programme page")
                    .replace("please verify on official programme page", "verify on official programme page");
            if (normalized.add(canonical)) {
                result.add(item);
            }
        }
        return result;
    }

    private List<String> dedupeStrings(List<String> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private String rankingCategory(int index) {
        if (index == 0) {
            return "Best match";
        }
        if (index < 3) {
            return "Strong alternative";
        }
        return "Explore option";
    }

    private String confidenceLevel(List<String> verifiedFacts, List<String> missingData) {
        if (verifiedFacts.size() >= 2 && missingData.isEmpty()) {
            return "HIGH";
        }
        if (!verifiedFacts.isEmpty()) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveSourceStatus(List<String> verifiedFacts,
                                       List<String> missingData,
                                       List<UniversitySourcePageResult> pages) {
        if (verifiedFacts.isEmpty() && pages.stream().anyMatch(page -> !page.success())) {
            return "FAILED";
        }
        if (!verifiedFacts.isEmpty() && !missingData.isEmpty()) {
            return "PARTIAL";
        }
        if (!verifiedFacts.isEmpty()) {
            return "SUCCESS";
        }
        return pages.isEmpty() ? "UNAVAILABLE" : "PARTIAL";
    }

    private String diagnosticStatus(UniversitySourcePageResult page) {
        if (!page.success() && page.failureType() != null && page.failureType().name().equalsIgnoreCase("TIMEOUT")) {
            return "TIMEOUT";
        }
        if (!page.success()) {
            return "FAILED";
        }
        return isProgrammeUsable(page) ? "SUCCESS" : "PARTIAL";
    }

    private boolean isProgrammeUsable(UniversitySourcePageResult page) {
        if (page == null || !page.success()) {
            return false;
        }
        return page.pageType() == UniversityPageType.PROGRAMME_DETAIL
                || page.pageType() == UniversityPageType.QUALIFICATION_LIST
                || page.pageType() == UniversityPageType.ADMISSIONS_OVERVIEW
                || page.pageType() == UniversityPageType.FEES_FUNDING;
    }

    private String fallbackFailureReason(UniversitySourcePageResult page) {
        if (page.success() && !isProgrammeUsable(page)) {
            return "Source fetched successfully but only provided partial or general information.";
        }
        return page.failureReason();
    }

    private String summarizeFact(UniversitySourcePageResult page) {
        if (!page.headings().isEmpty()) {
            return page.pageType() + " page referencing " + String.join(", ", page.headings().stream().limit(2).toList());
        }
        if (page.pageTitle() != null && !page.pageTitle().isBlank()) {
            return page.pageType() + " page titled '" + page.pageTitle() + "'";
        }
        return page.pageType() + " page from official source";
    }

    private String inferUniversity(String sourceUrl) {
        try {
            URI uri = URI.create(sourceUrl);
            if (uri.getHost() == null) {
                return "Official university source";
            }
            String host = uri.getHost().replaceFirst("^www\\.", "");
            String label = host.split("\\.")[0].replace('-', ' ').trim();
            if (label.isBlank()) {
                return host;
            }
            return java.util.Arrays.stream(label.split("\\s+"))
                    .map(this::capitalize)
                    .collect(Collectors.joining(" "));
        } catch (RuntimeException ex) {
            return "Official university source";
        }
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        if (haystack == null || needle == null || haystack.isBlank() || needle.isBlank()) {
            return false;
        }
        return normalize(haystack).contains(normalize(needle));
    }

    private boolean containsAny(String value, List<String> needles) {
        return needles.stream().anyMatch(needle -> normalize(value).contains(normalize(needle)));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
