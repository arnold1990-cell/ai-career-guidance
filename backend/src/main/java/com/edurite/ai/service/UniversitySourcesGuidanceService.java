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

        int targetSourceLimit = request.usesDefaultSources()
                ? Math.min(Math.max(registryService.configuredUniversityCount() * 2, 24), 120)
                : Math.min(Math.max(registryService.configuredUniversityCount() * 2, 40), 150);

        List<String> urls = resolveUrls(profile, request, targetSourceLimit);
        List<UniversitySourcePageResult> fetchedPages = fetchPagesSafely(urls, request.usesDefaultSources());
        String combinedContext = buildCombinedContextSafely(fetchedPages, profile, request);
        UniversitySourcesAnalysisResponse baseResponse = geminiService.getUniversitySourcesAdvice(request, profile, urls, fetchedPages, combinedContext);
        UniversitySourcesAnalysisResponse response = enrichSafely(baseResponse, request, profile, urls, fetchedPages);

        log.info("University analysis completed: requestedByDefaultSources={}, discoveredUrlCount={}, fetchedPages={}, successfulPages={}, combinedContextLength={}, durationMs={}",
                request.usesDefaultSources(),
                urls.size(),
                fetchedPages.size(),
                fetchedPages.stream().filter(UniversitySourcePageResult::success).count(),
                combinedContext.length(),
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
