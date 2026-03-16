package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourcesGuidanceService {

    private static final int AUTO_RETRIEVAL_LIMIT = 12;
    private static final Logger log = LoggerFactory.getLogger(UniversitySourcesGuidanceService.class);

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
        log.info("Starting university source analysis service: principalPresent={}, usesDefaultSources={}, incomingUrls={}",
                principal != null,
                request != null && request.usesDefaultSources(),
                request == null || request.urls() == null ? 0 : request.urls().size());
        try {
            if (request == null) {
                throw new AiServiceException(HttpStatus.BAD_REQUEST, "Request payload is required");
            }

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
                                    null,
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
                    log.info("University source analysis using retrieval service output: summaries={}, urls={}", summaries.size(), urls.size());
                    return geminiService.getUniversitySourcesAdvice(request, profile, urls, retrievedPages, combinedContext);
                }
                log.warn("University page retrieval returned no summaries; falling back to source fetch flow.");
            }

            List<String> urls = (request.urls() == null || request.urls().isEmpty())
                    ? registryService.getDefaultSources()
                    : request.urls();
            urls = registryService.deduplicate(urls).stream().limit(30).toList();

            if (urls.isEmpty()) {
                throw new AiServiceException(HttpStatus.BAD_REQUEST, "No university source URLs were provided or resolved");
            }

            List<UniversitySourcePageResult> fetchedPages = pageFetcherService.fetchPages(urls);
            String combinedContext = aggregatorService.buildCombinedContext(fetchedPages, profile, request);
            log.info("University source pages fetched: requestedUrls={}, fetchedPages={}", urls.size(), fetchedPages.size());

            UniversitySourcesAnalysisResponse response = geminiService.getUniversitySourcesAdvice(request, profile, urls, fetchedPages, combinedContext);
            log.info("University source analysis response generated: recommendedCareers={}, recommendedProgrammes={}",
                    response.recommendedCareers() == null ? 0 : response.recommendedCareers().size(),
                    response.recommendedProgrammes() == null ? 0 : response.recommendedProgrammes().size());
            return response;
        } catch (AiServiceException ex) {
            log.warn("University source analysis failed with domain error: status={}, message={}", ex.getStatus().value(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected failure in university source analysis service.", ex);
            throw new AiServiceException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "University source analysis failed due to an internal error: " + ex.getMessage());
        }
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
