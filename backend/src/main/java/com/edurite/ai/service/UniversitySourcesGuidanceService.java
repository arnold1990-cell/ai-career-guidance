package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.MultiUniversityPageFetcherService;
import com.edurite.ai.university.PublicUniversitySourceDiscoveryService;
import com.edurite.ai.university.UniversityPageRetrievalService;
import com.edurite.ai.university.UniversityPageSummary;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.ai.university.UniversitySourcesAggregatorService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourcesGuidanceService {

    private static final Logger log = LoggerFactory.getLogger(UniversitySourcesGuidanceService.class);
    private static final int AUTO_RETRIEVAL_LIMIT = 12;

    private final UniversitySourceRegistryService registryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final PublicUniversitySourceDiscoveryService discoveryService;
    private final UniversitySourcesAggregatorService aggregatorService;
    private final UniversityPageRetrievalService retrievalService;
    private final StudentService studentService;
    private final GeminiService geminiService;

    public UniversitySourcesGuidanceService(UniversitySourceRegistryService registryService,
                                            MultiUniversityPageFetcherService pageFetcherService,
                                            PublicUniversitySourceDiscoveryService discoveryService,
                                            UniversitySourcesAggregatorService aggregatorService,
                                            UniversityPageRetrievalService retrievalService,
                                            StudentService studentService,
                                            GeminiService geminiService) {
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.discoveryService = discoveryService;
        this.aggregatorService = aggregatorService;
        this.retrievalService = retrievalService;
        this.studentService = studentService;
        this.geminiService = geminiService;
    }

    public UniversitySourcesAnalysisResponse analyse(Principal principal, UniversitySourcesAnalysisRequest request) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        List<String> urls = new ArrayList<>();

        if (request.usesDefaultSources()) {
            urls.addAll(discoveryService.discoverPublicUniversityUrls(profile, request, AUTO_RETRIEVAL_LIMIT));
            List<UniversityPageSummary> summaries = retrievalService.retrieveTopRelevantPages(profile, request, AUTO_RETRIEVAL_LIMIT);
            urls.addAll(summaries.stream().map(UniversityPageSummary::sourceUrl).toList());
            urls = registryService.deduplicate(urls).stream().limit(30).toList();
            log.info("University source discovery produced {} URLs and {} stored summaries.", urls.size(), summaries.size());
        } else {
            urls = registryService.deduplicate(request.urls()).stream().limit(30).toList();
        }

        if (urls.isEmpty()) {
            urls = registryService.getDefaultSources().stream().limit(12).toList();
        }

        List<UniversitySourcePageResult> fetchedPages = pageFetcherService.fetchPages(urls);
        String combinedContext = aggregatorService.buildCombinedContext(fetchedPages, profile, request);
        return geminiService.getUniversitySourcesAdvice(request, profile, urls, fetchedPages, combinedContext);
    }

    public List<String> getDefaultSources() {
        return registryService.getDefaultSources();
    }
}
