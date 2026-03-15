package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.MultiUniversityPageFetcherService;
import com.edurite.ai.university.UniversityPageRetrievalService;
import com.edurite.ai.university.UniversityPageSummary;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.ai.university.UniversitySourceRegistryService;
import com.edurite.ai.university.UniversitySourcesAggregatorService;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourcesGuidanceService {

    private static final int AUTO_RETRIEVAL_LIMIT = 12;

    private final UniversitySourceRegistryService registryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final UniversitySourcesAggregatorService aggregatorService;
    private final UniversityPageRetrievalService retrievalService;
    private final StudentService studentService;
    private final GeminiService geminiService;

    public UniversitySourcesGuidanceService(
            UniversitySourceRegistryService registryService,
            MultiUniversityPageFetcherService pageFetcherService,
            UniversitySourcesAggregatorService aggregatorService,
            UniversityPageRetrievalService retrievalService,
            StudentService studentService,
            GeminiService geminiService
    ) {
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.aggregatorService = aggregatorService;
        this.retrievalService = retrievalService;
        this.studentService = studentService;
        this.geminiService = geminiService;
    }

    public UniversitySourcesAnalysisResponse analyse(Principal principal, UniversitySourcesAnalysisRequest request) {
        StudentProfile profile = studentService.getProfileEntity(principal);

        if (request.usesDefaultSources()) {
            List<UniversityPageSummary> summaries = retrievalService.retrieveTopRelevantPages(profile, request, AUTO_RETRIEVAL_LIMIT);
            if (!summaries.isEmpty()) {
                List<UniversitySourcePageResult> retrievedPages = summaries.stream()
                        .map(summary -> new UniversitySourcePageResult(
                                summary.sourceUrl(),
                                summary.pageTitle(),
                                parsePageType(summary.pageType()),
                                summary.summaryExcerpt(),
                                summary.keywords(),
                                true,
                                null
                        ))
                        .toList();
                String combinedContext = summaries.stream()
                        .map(summary -> "University: " + summary.universityName()
                                + "\nTitle: " + summary.pageTitle()
                                + "\nType: " + summary.pageType()
                                + "\nQualification: " + summary.qualificationLevel()
                                + "\nKeywords: " + String.join(", ", summary.keywords())
                                + "\nExcerpt: " + summary.summaryExcerpt())
                        .reduce("", (left, right) -> left + "\n\n" + right)
                        .trim();
                List<String> urls = summaries.stream().map(UniversityPageSummary::sourceUrl).toList();
                return geminiService.getUniversitySourcesAdvice(request, profile, urls, retrievedPages, combinedContext);
            }
        }

        List<String> urls = (request.urls() == null || request.urls().isEmpty())
                ? registryService.getDefaultSources()
                : request.urls();
        urls = registryService.deduplicate(urls).stream().limit(30).toList();

        List<UniversitySourcePageResult> fetchedPages = pageFetcherService.fetchPages(urls);
        String combinedContext = aggregatorService.buildCombinedContext(fetchedPages, profile, request);

        return geminiService.getUniversitySourcesAdvice(request, profile, urls, fetchedPages, combinedContext);
    }

    public List<String> getDefaultSources() {
        return registryService.getDefaultSources();
    }

    private com.edurite.ai.university.UniversityPageType parsePageType(String value) {
        try {
            return com.edurite.ai.university.UniversityPageType.valueOf(value);
        } catch (Exception ex) {
            return com.edurite.ai.university.UniversityPageType.UNKNOWN;
        }
    }
}
