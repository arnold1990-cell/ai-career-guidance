package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.MultiUniversityPageFetcherService;
import com.edurite.ai.university.PublicUniversitySourceDiscoveryService;
import com.edurite.ai.university.UniversityCrawlFailureType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.ai.university.UniversitySourcesAggregatorService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
    private final UniversityStructuredRecommendationService structuredRecommendationService;

    public UniversitySourcesGuidanceService(
            UniversitySourceRegistryService registryService,
            PublicUniversitySourceDiscoveryService discoveryService,
            MultiUniversityPageFetcherService pageFetcherService,
            UniversitySourcesAggregatorService aggregatorService,
            StudentService studentService,
            GeminiService geminiService,
            UniversityGuidanceResultEnricher resultEnricher,
            UniversityStructuredRecommendationService structuredRecommendationService
    ) {
        this.registryService = registryService;
        this.discoveryService = discoveryService;
        this.pageFetcherService = pageFetcherService;
        this.aggregatorService = aggregatorService;
        this.studentService = studentService;
        this.geminiService = geminiService;
        this.resultEnricher = resultEnricher;
        this.structuredRecommendationService = structuredRecommendationService;
    }

    public UniversitySourcesAnalysisResponse analyse(Principal principal, UniversitySourcesAnalysisRequest request) {
        Instant startedAt = Instant.now();
        StudentProfile profile = studentService.getProfileEntity(principal);
        int targetSourceLimit = request.usesDefaultSources()
                ? Math.min(Math.max(registryService.configuredUniversityCount() * 2, 24), 120)
                : Math.min(Math.max(registryService.configuredUniversityCount() * 2, 40), 150);

        Instant crawlerStart = Instant.now();
        List<String> urls = resolveUrls(profile, request, targetSourceLimit);
        List<UniversitySourcePageResult> fetchedPages = fetchPagesSafely(urls, request.usesDefaultSources());
        String combinedContext = buildCombinedContextSafely(fetchedPages, profile, request);
        long crawlerDurationMs = Duration.between(crawlerStart, Instant.now()).toMillis();

        Instant aiStart = Instant.now();
        UniversitySourcesAnalysisResponse response = analyseWithFallback(request, profile, urls, fetchedPages, combinedContext, crawlerDurationMs);
        long aiDurationMs = Duration.between(aiStart, Instant.now()).toMillis();
        UniversitySourcesAnalysisResponse.AnalysisDiagnostics diagnostics = buildDiagnostics(urls, fetchedPages, crawlerDurationMs, aiDurationMs);
        UniversitySourcesAnalysisResponse responseWithDiagnostics = attachDiagnostics(response, diagnostics);
        UniversitySourcesAnalysisResponse enriched = enrichSafely(responseWithDiagnostics, request, profile, urls, fetchedPages);

        log.info("University analysis completed: status={}, mode={}, requestedByDefaultSources={}, discoveredUrlCount={}, fetchedPages={}, successfulPages={}, combinedContextLength={}, crawlerDurationMs={}, aiDurationMs={}, durationMs={}",
                enriched.status(),
                enriched.mode(),
                request.usesDefaultSources(),
                urls.size(),
                fetchedPages.size(),
                fetchedPages.stream().filter(UniversitySourcePageResult::success).count(),
                combinedContext.length(),
                crawlerDurationMs,
                aiDurationMs,
                Duration.between(startedAt, Instant.now()).toMillis());
        return enriched;
    }

    private UniversitySourcesAnalysisResponse analyseWithFallback(UniversitySourcesAnalysisRequest request,
                                                                  StudentProfile profile,
                                                                  List<String> urls,
                                                                  List<UniversitySourcePageResult> fetchedPages,
                                                                  String combinedContext,
                                                                  long crawlerDurationMs) {
        try {
            UniversitySourcesAnalysisResponse baseResponse = geminiService.getUniversitySourcesAdvice(request, profile, urls, fetchedPages, combinedContext);
            if (!Boolean.TRUE.equals(baseResponse.fallbackUsed())) {
                return classifySuccess(baseResponse, fetchedPages, crawlerDurationMs);
            }
            log.warn("University guidance live AI unavailable or failed; switching to registry-driven deterministic recommendations. message={}", baseResponse.warningMessage());
        } catch (RuntimeException ex) {
            log.error("University guidance AI pipeline failed, switching to registry-driven deterministic recommendations. message={}", ex.getMessage(), ex);
        }

        UniversitySourcesAnalysisResponse.AnalysisDiagnostics provisionalDiagnostics = buildDiagnostics(urls, fetchedPages, crawlerDurationMs, 0);
        if (hasUsableResults(fetchedPages)) {
            return structuredRecommendationService.buildResponse(
                    request,
                    profile,
                    urls,
                    fetchedPages,
                    provisionalDiagnostics,
                    fetchedPages.stream().anyMatch(page -> !page.success()) ? "PARTIAL" : "SUCCESS",
                    fetchedPages.stream().anyMatch(page -> !page.success()) ? "PARTIAL" : "LIVE",
                    fetchedPages.stream().anyMatch(page -> !page.success())
                            ? "Some official institution sources failed, but partial guidance is available from curated official pages."
                            : "Guidance was assembled from curated official institution pages."
            );
        }
        return structuredRecommendationService.buildResponse(
                request,
                profile,
                urls,
                fetchedPages,
                provisionalDiagnostics,
                "ERROR",
                "UNAVAILABLE",
                "AI Guidance is currently unavailable because no usable official institution sources could be analysed for this request."
        );
    }

    private UniversitySourcesAnalysisResponse classifySuccess(UniversitySourcesAnalysisResponse response,
                                                              List<UniversitySourcePageResult> fetchedPages,
                                                              long crawlerDurationMs) {
        boolean partial = fetchedPages.stream().anyMatch(page -> !page.success()) || crawlerDurationMs > 20_000;
        return new UniversitySourcesAnalysisResponse(
                partial ? "PARTIAL" : "SUCCESS",
                response.aiLive(),
                response.fallbackUsed(),
                partial ? "PARTIAL" : "LIVE",
                partial ? "Guidance is available, but some official institution sources could not be processed." : "Guidance generated successfully from official institution sources.",
                response.groundingStatus(),
                response.evidenceCoverage(),
                response.warningMessage(),
                response.requestedSources(),
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
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
                response.warnings(),
                response.suitabilityScore(),
                response.rawModelUsed(),
                response.suitabilityScoreReason(),
                response.suitabilitySignalsUsed(),
                response.suitabilityScoreLimitations(),
                response.sourceDiagnostics(),
                response.sourceCoverage(),
                response.diagnostics()
        );
    }

    private UniversitySourcesAnalysisResponse attachDiagnostics(UniversitySourcesAnalysisResponse response,
                                                                UniversitySourcesAnalysisResponse.AnalysisDiagnostics diagnostics) {
        return new UniversitySourcesAnalysisResponse(
                response.status(),
                response.aiLive(),
                response.fallbackUsed(),
                response.mode(),
                response.message(),
                response.groundingStatus(),
                response.evidenceCoverage(),
                response.warningMessage(),
                response.requestedSources(),
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
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
                response.warnings(),
                response.suitabilityScore(),
                response.rawModelUsed(),
                response.suitabilityScoreReason(),
                response.suitabilitySignalsUsed(),
                response.suitabilityScoreLimitations(),
                response.sourceDiagnostics(),
                response.sourceCoverage(),
                diagnostics
        );
    }

    private UniversitySourcesAnalysisResponse.AnalysisDiagnostics buildDiagnostics(List<String> urls,
                                                                                   List<UniversitySourcePageResult> fetchedPages,
                                                                                   long crawlerDurationMs,
                                                                                   long aiDurationMs) {
        int technicalFailures = (int) fetchedPages.stream().filter(page -> page.failureType() == UniversityCrawlFailureType.NETWORK_ERROR
                || page.failureType() == UniversityCrawlFailureType.SSL_ERROR
                || page.failureType() == UniversityCrawlFailureType.FETCH_ERROR).count();
        int protectedSources = (int) fetchedPages.stream().filter(page -> page.failureType() == UniversityCrawlFailureType.PROTECTED
                || page.failureType() == UniversityCrawlFailureType.FORBIDDEN).count();
        int timeouts = (int) fetchedPages.stream().filter(page -> page.failureType() == UniversityCrawlFailureType.TIMEOUT).count();
        int usable = (int) fetchedPages.stream().filter(UniversitySourcePageResult::success).count();
        int rejected = (int) fetchedPages.stream().filter(page -> !page.success()).count();
        int succeededInstitutions = (int) fetchedPages.stream().filter(UniversitySourcePageResult::success)
                .map(page -> registryService.inferInstitutionName(page.sourceUrl())).distinct().count();
        int requestedInstitutions = (int) urls.stream().map(registryService::inferInstitutionName).distinct().count();
        int attemptedInstitutions = (int) fetchedPages.stream().map(page -> registryService.inferInstitutionName(page.sourceUrl())).distinct().count();
        return new UniversitySourcesAnalysisResponse.AnalysisDiagnostics(
                requestedInstitutions,
                attemptedInstitutions,
                succeededInstitutions,
                usable,
                rejected,
                technicalFailures,
                protectedSources,
                timeouts,
                crawlerDurationMs,
                aiDurationMs
        );
    }

    private boolean hasUsableResults(List<UniversitySourcePageResult> fetchedPages) {
        return fetchedPages.stream().anyMatch(UniversitySourcePageResult::success);
    }

    private List<String> resolveUrls(StudentProfile profile,
                                     UniversitySourcesAnalysisRequest request,
                                     int targetSourceLimit) {
        try {
            List<String> urls = request.usesDefaultSources()
                    ? discoveryService.discoverSources(profile, request, targetSourceLimit)
                    : registryService.deduplicate(request.urls()).stream().limit(targetSourceLimit).toList();
            log.info("University source discovery completed: requestedByDefaultSources={}, discoveredUrlCount={}",
                    request.usesDefaultSources(), urls.size());
            return urls;
        } catch (RuntimeException ex) {
            log.error("University source discovery failed: requestedByDefaultSources={}, targetSourceLimit={}, message={}",
                    request.usesDefaultSources(), targetSourceLimit, ex.getMessage(), ex);
            return List.of();
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
            return List.of();
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
            return "";
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

    public List<String> getDefaultSources() {
        return registryService.getDefaultSources();
    }
}
