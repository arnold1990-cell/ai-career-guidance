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
        StudentProfile profile = studentService.getProfileEntity(principal);

        int targetSourceLimit = request.usesDefaultSources()
                ? Math.min(Math.max(registryService.configuredUniversityCount() * 2, 24), 120)
                : Math.min(Math.max(registryService.configuredUniversityCount() * 2, 40), 150);

        List<String> urls = request.usesDefaultSources()
                ? discoveryService.discoverSources(profile, request, targetSourceLimit)
                : registryService.deduplicate(request.urls()).stream().limit(targetSourceLimit).toList();

        log.info("University source discovery completed: requestedByDefaultSources={}, discoveredUrlCount={}", request.usesDefaultSources(), urls.size());

        List<UniversitySourcePageResult> fetchedPages = pageFetcherService.fetchPages(urls);
        String combinedContext = aggregatorService.buildCombinedContext(fetchedPages, profile, request);
        return resultEnricher.enrich(
                geminiService.getUniversitySourcesAdvice(request, profile, urls, fetchedPages, combinedContext),
                request,
                profile,
                urls,
                fetchedPages
        );
    }

    public List<String> getDefaultSources() {
        return registryService.getDefaultSources();
    }
}
