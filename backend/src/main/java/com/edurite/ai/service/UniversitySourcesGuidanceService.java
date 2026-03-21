package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.MultiUniversityPageFetcherService;
import com.edurite.ai.university.PublicUniversitySourceDiscoveryService;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.ai.university.UniversitySourcesAggregatorService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourcesGuidanceService {

    private static final Logger log = LoggerFactory.getLogger(UniversitySourcesGuidanceService.class);

    private final UniversitySourceRegistryService registryService;
    private final PublicUniversitySourceDiscoveryService discoveryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final UniversitySourcesAggregatorService aggregatorService;
    private final StudentService studentService;
    private final GeminiService geminiService;
    private final UniversityGuidanceResultEnricher resultEnricher;

    public UniversitySourcesGuidanceService(
            UniversitySourceRegistryService registryService,
            PublicUniversitySourceDiscoveryService discoveryService,
            MultiUniversityPageFetcherService pageFetcherService,
            UniversitySourcesAggregatorService aggregatorService,
            StudentService studentService,
            GeminiService geminiService,
            UniversityGuidanceResultEnricher resultEnricher
    ) {
        this.registryService = registryService;
        this.discoveryService = discoveryService;
        this.pageFetcherService = pageFetcherService;
        this.aggregatorService = aggregatorService;
        this.studentService = studentService;
        this.geminiService = geminiService;
        this.resultEnricher = resultEnricher;
    }

    public UniversitySourcesAnalysisResponse analyse(Principal principal, UniversitySourcesAnalysisRequest request) {
        Instant startedAt = Instant.now();
        StudentProfile profile = studentService.getProfileEntity(principal);

        int registrySize = registryService.configuredUniversityCount();
        List<com.edurite.ai.university.UniversityRegistryProperties.UniversityRegistryEntry> activeUniversityEntries = registryService.getActiveUniversities();
        int activeInstitutions = activeUniversityEntries.size();
        log.info("University analysis profile loaded: principalPresent={}, location={}, qualificationLevel={}, interests={}, skills={}, targetProgram={}, careerInterest={}",
                principal != null,
                sanitize(profile.getLocation()),
                sanitize(profile.getQualificationLevel()),
                sanitize(profile.getInterests()),
                sanitize(profile.getSkills()),
                sanitize(request.targetProgram()),
                sanitize(request.careerInterest()));
        int targetSourceLimit = request.usesDefaultSources()
                ? Math.min(Math.max(registrySize * 2, 24), 120)
                : Math.min(Math.max(registrySize * 2, 40), 150);

        log.info("University analysis pipeline starting: registrySize={}, activeInstitutions={}, usesDefaultSources={}, requestedSources={}, targetSourceLimit={}",
                registrySize,
                activeInstitutions,
                request.usesDefaultSources(),
                request.urls() == null ? 0 : request.urls().size(),
                targetSourceLimit);
        if (activeInstitutions > 0) {
            long institutionsWithCuratedSources = activeUniversityEntries.stream()
                    .filter(entry -> !registryService.curatedSourcesFor(entry, 8).isEmpty())
                    .count();
            log.info("University registry filtering completed: institutionsLoaded={}, institutionsWithCuratedOfficialSources={}",
                    activeInstitutions, institutionsWithCuratedSources);
        }

        List<String> urls = resolveUrls(profile, request, targetSourceLimit);
        List<UniversitySourcePageResult> fetchedPages = fetchPagesSafely(urls, request.usesDefaultSources());
        String combinedContext = buildCombinedContextSafely(fetchedPages, profile, request);
        log.info("Gemini analysis starting: requestedSources={}, successfulFetchedPages={}, combinedContextLength={}",
                urls.size(), fetchedPages.stream().filter(UniversitySourcePageResult::success).count(), combinedContext.length());
        UniversitySourcesAnalysisResponse baseResponse = geminiService.getUniversitySourcesAdvice(request, profile, urls, fetchedPages, combinedContext);
        log.info("Gemini analysis completed: fallbackUsed={}, returnedMode={}, returnedStatus={}",
                baseResponse.fallbackUsed(), baseResponse.mode(), baseResponse.status());
        UniversitySourcesAnalysisResponse response = enrichSafely(baseResponse, request, profile, urls, fetchedPages);
        response = applyPipelineStatus(response, urls, fetchedPages);

        long successfulPages = fetchedPages.stream().filter(UniversitySourcePageResult::success).count();
        log.info("University analysis completed: registrySize={}, activeInstitutions={}, requestedSources={}, discoveredUrlCount={}, fetchedPages={}, successfulSources={}, failedSources={}, mode={}, durationMs={}",
                registrySize,
                activeInstitutions,
                request.urls() == null ? 0 : request.urls().size(),
                urls.size(),
                fetchedPages.size(),
                successfulPages,
                Math.max(0, fetchedPages.size() - (int) successfulPages),
                response.mode(),
                Duration.between(startedAt, Instant.now()).toMillis());
        return response;
    }

    private List<String> resolveUrls(StudentProfile profile,
                                     UniversitySourcesAnalysisRequest request,
                                     int targetSourceLimit) {
        try {
            List<String> urls = request.usesDefaultSources()
                    ? discoveryService.discoverSources(profile, request, targetSourceLimit)
                    : registryService.deduplicate(request.urls()).stream().limit(targetSourceLimit).toList();
            if (urls.isEmpty()) {
                urls = registryService.getFallbackSources(targetSourceLimit);
                log.warn("University source discovery returned zero URLs; using registry fallback sources: requestedByDefaultSources={}, fallbackUrls={}",
                        request.usesDefaultSources(), urls.size());
            }
            log.info("University source discovery completed: requestedByDefaultSources={}, discoveredUrlCount={}, sampleDiscoveredUrls={}, registryFallbackUsed={}",
                    request.usesDefaultSources(), urls.size(), urls.stream().limit(3).toList(), !urls.isEmpty());
            return urls;
        } catch (RuntimeException ex) {
            List<String> fallbackUrls = registryService.getFallbackSources(targetSourceLimit);
            log.error("University source discovery failed: requestedByDefaultSources={}, targetSourceLimit={}, fallbackUrls={}, message={}",
                    request.usesDefaultSources(), targetSourceLimit, fallbackUrls.size(), ex.getMessage(), ex);
            return fallbackUrls;
        }
    }

    private List<UniversitySourcePageResult> fetchPagesSafely(List<String> urls, boolean requestedByDefaultSources) {
        try {
            List<UniversitySourcePageResult> fetchedPages = pageFetcherService.fetchPages(urls);
            log.info("University page fetch completed: requestedByDefaultSources={}, requestedUrls={}, fetchedPages={}, successfulPages={}, failedPages={}",
                    requestedByDefaultSources,
                    urls.size(),
                    fetchedPages.size(),
                    fetchedPages.stream().filter(UniversitySourcePageResult::success).count(),
                    fetchedPages.stream().filter(page -> !page.success()).count());
            return fetchedPages;
        } catch (RuntimeException ex) {
            log.error("University page fetch failed: requestedByDefaultSources={}, requestedUrls={}, message={}",
                    requestedByDefaultSources, urls.size(), ex.getMessage(), ex);
            return buildFailedFetchResults(urls, "Fetch pipeline failed before individual source results were recorded.");
        }
    }

    private String buildCombinedContextSafely(List<UniversitySourcePageResult> fetchedPages,
                                              StudentProfile profile,
                                              UniversitySourcesAnalysisRequest request) {
        try {
            String combinedContext = aggregatorService.buildCombinedContext(fetchedPages, profile, request);
            log.info("University source aggregation completed: fetchedPages={}, combinedContextLength={}",
                    fetchedPages.size(), combinedContext.length());
            return combinedContext;
        } catch (RuntimeException ex) {
            log.error("University source aggregation failed: fetchedPages={}, message={}",
                    fetchedPages.size(), ex.getMessage(), ex);
            String partialContext = fetchedPages.stream()
                    .filter(UniversitySourcePageResult::success)
                    .map(page -> page.pageTitle() + "\n" + page.cleanedText())
                    .filter(value -> value != null && !value.isBlank())
                    .limit(3)
                    .reduce("", (left, right) -> left + "\n\n" + right)
                    .trim();
            if (!partialContext.isBlank()) {
                log.warn("Using simplified combined context fallback after aggregation failure: fallbackContextLength={}", partialContext.length());
            }
            return partialContext;
        }
    }

    private UniversitySourcesAnalysisResponse enrichSafely(UniversitySourcesAnalysisResponse response,
                                                           UniversitySourcesAnalysisRequest request,
                                                           StudentProfile profile,
                                                           List<String> urls,
                                                           List<UniversitySourcePageResult> fetchedPages) {
        try {
            return resultEnricher.enrich(response, request, profile, urls, fetchedPages);
        } catch (RuntimeException ex) {
            log.error("University guidance enrichment failed: urls={}, fetchedPages={}, message={}",
                    urls.size(), fetchedPages.size(), ex.getMessage(), ex);
            return response;
        }
    }

    private UniversitySourcesAnalysisResponse applyPipelineStatus(UniversitySourcesAnalysisResponse response,
                                                                  List<String> urls,
                                                                  List<UniversitySourcePageResult> fetchedPages) {
        List<String> successfulUrls = fetchedPages.stream()
                .filter(UniversitySourcePageResult::success)
                .map(UniversitySourcePageResult::sourceUrl)
                .toList();
        List<String> failedUrls = fetchedPages.stream()
                .filter(page -> !page.success())
                .map(UniversitySourcePageResult::sourceUrl)
                .toList();
        boolean hasRequestedSources = !urls.isEmpty();
        boolean hasSuccessfulSources = !successfulUrls.isEmpty();
        boolean hasFailures = !failedUrls.isEmpty() || (hasRequestedSources && successfulUrls.size() < urls.size());

        String mode = response.mode();
        String warningMessage = response.warningMessage();
        if (hasSuccessfulSources && hasFailures) {
            mode = "PARTIAL";
            warningMessage = mergeWarning(warningMessage,
                    "Some university sources could not be analysed, so EduRite returned the successful official sources that were available.");
        } else if (hasSuccessfulSources) {
            mode = response.fallbackUsed() ? "FALLBACK" : "LIVE";
        } else if (hasRequestedSources) {
            mode = "PARTIAL";
            warningMessage = mergeWarning(warningMessage,
                    "University sources were requested, but no official pages could be analysed successfully. EduRite kept the official source list and returned the best available profile-based guidance instead of failing completely.");
        } else {
            mode = "UNAVAILABLE";
        }

        Set<String> requestedSources = new LinkedHashSet<>();
        requestedSources.addAll(response.requestedSources() == null ? List.of() : response.requestedSources());
        requestedSources.addAll(urls);

        List<String> warnings = new ArrayList<>();
        if (response.warnings() != null) {
            warnings.addAll(response.warnings());
        }
        if (hasFailures) {
            warnings.add("Some requested university sources were unavailable or only partially usable, so EduRite continued with the successful sources.");
        }

        return new UniversitySourcesAnalysisResponse(
                response.aiLive(),
                response.fallbackUsed(),
                deriveStatus(mode, hasSuccessfulSources, hasRequestedSources),
                mode,
                response.groundingStatus(),
                response.evidenceCoverage(),
                warningMessage,
                List.copyOf(requestedSources),
                urls,
                successfulUrls,
                failedUrls,
                successfulUrls.size(),
                response.summary(),
                response.inferredGuidance(),
                response.recommendedCareers(),
                response.recommendedProgrammes(),
                response.bursarySuggestions(),
                response.recommendedUniversities(),
                response.minimumRequirements(),
                response.keyRequirements(),
                response.skillGaps(),
                response.recommendedNextSteps(),
                warnings,
                response.suitabilityScore(),
                response.rawModelUsed(),
                response.suitabilityScoreReason(),
                response.suitabilitySignalsUsed(),
                response.suitabilityScoreLimitations(),
                response.sourceDiagnostics(),
                response.sourceCoverage()
        );
    }

    private String deriveStatus(String mode, boolean hasSuccessfulSources, boolean hasRequestedSources) {
        if ("PARTIAL".equalsIgnoreCase(mode) || (hasSuccessfulSources && hasRequestedSources)) {
            return "PARTIAL";
        }
        if ("LIVE".equalsIgnoreCase(mode) || "FALLBACK".equalsIgnoreCase(mode)) {
            return "SUCCESS";
        }
        return hasRequestedSources ? "PARTIAL" : "ERROR";
    }

    private List<UniversitySourcePageResult> buildFailedFetchResults(List<String> urls, String failureReason) {
        return urls.stream()
                .map(url -> new UniversitySourcePageResult(url, "", com.edurite.ai.university.UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false,
                        failureReason, com.edurite.ai.university.UniversityCrawlFailureType.FETCH_ERROR))
                .toList();
    }

    private String mergeWarning(String currentWarning, String additionalWarning) {
        if (additionalWarning == null || additionalWarning.isBlank()) {
            return currentWarning;
        }
        if (currentWarning == null || currentWarning.isBlank()) {
            return additionalWarning;
        }
        if (currentWarning.contains(additionalWarning)) {
            return currentWarning;
        }
        return currentWarning + " " + additionalWarning;
    }

    public List<String> getDefaultSources() {
        return registryService.getDefaultSources();
    }

    private String sanitize(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
