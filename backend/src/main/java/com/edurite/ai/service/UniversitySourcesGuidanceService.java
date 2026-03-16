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
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
    private final UniversityContextService universityContextService;

    public UniversitySourcesGuidanceService(
            UniversitySourceRegistryService registryService,
            MultiUniversityPageFetcherService pageFetcherService,
            UniversitySourcesAggregatorService aggregatorService,
            UniversityPageRetrievalService retrievalService,
            StudentService studentService,
            GeminiService geminiService,
            UniversityContextService universityContextService
    ) {
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.aggregatorService = aggregatorService;
        this.retrievalService = retrievalService;
        this.studentService = studentService;
        this.geminiService = geminiService;
        this.universityContextService = universityContextService;
    }

    public UniversitySourcesAnalysisResponse analyse(Principal principal, UniversitySourcesAnalysisRequest request) {
        StudentProfile profile = studentService.getProfileEntity(principal);
        String educationQuery = String.join(" ",
                safe(request.targetProgram()),
                safe(request.careerInterest()),
                safe(request.qualificationLevel()),
                safe(profile.getInterests()),
                safe(profile.getCareerGoals()));
        UniversityContextService.UniversityContextResult universityContext = universityContextService.buildContext(profile, educationQuery, 8);

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
                UniversitySourcesAnalysisResponse response = geminiService.getUniversitySourcesAdvice(
                        request,
                        profile,
                        urls,
                        retrievedPages,
                        combinedContext,
                        universityContext.promptContext());
                return mergeWithUniversityModule(response, universityContext);
            }
        }

        List<String> urls = (request.urls() == null || request.urls().isEmpty())
                ? registryService.getDefaultSources()
                : request.urls();
        urls = registryService.deduplicate(urls).stream().limit(30).toList();

        List<UniversitySourcePageResult> fetchedPages = pageFetcherService.fetchPages(urls);
        String combinedContext = aggregatorService.buildCombinedContext(fetchedPages, profile, request);

        UniversitySourcesAnalysisResponse response = geminiService.getUniversitySourcesAdvice(
                request,
                profile,
                urls,
                fetchedPages,
                combinedContext,
                universityContext.promptContext());
        return mergeWithUniversityModule(response, universityContext);
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

    private UniversitySourcesAnalysisResponse mergeWithUniversityModule(
            UniversitySourcesAnalysisResponse response,
            UniversityContextService.UniversityContextResult universityContext
    ) {
        LinkedHashSet<String> mergedUniversities = new LinkedHashSet<>();
        mergedUniversities.addAll(universityContext.universitySummaryLines());
        mergedUniversities.addAll(response.recommendedUniversities());

        LinkedHashSet<String> mergedRequirements = new LinkedHashSet<>();
        mergedRequirements.addAll(response.minimumRequirements());
        mergedRequirements.addAll(universityContext.entryRequirementLines());

        LinkedHashSet<String> warnings = new LinkedHashSet<>(response.warnings());
        if (universityContext.universities().isEmpty()) {
            warnings.add("No strong internal university match was found for this query. Refine the programme or career keyword.");
        }

        return new UniversitySourcesAnalysisResponse(
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
                response.summary(),
                response.recommendedCareers(),
                response.recommendedProgrammes(),
                new ArrayList<>(mergedUniversities),
                new ArrayList<>(mergedRequirements),
                response.keyRequirements(),
                response.skillGaps(),
                response.recommendedNextSteps(),
                new ArrayList<>(warnings),
                response.suitabilityScore(),
                response.rawModelUsed()
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
