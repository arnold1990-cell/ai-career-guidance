package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.MultiUniversityPageFetcherService;
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

    private final UniversitySourceRegistryService registryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final UniversitySourcesAggregatorService aggregatorService;
    private final StudentService studentService;
    private final GeminiService geminiService;

    public UniversitySourcesGuidanceService(
            UniversitySourceRegistryService registryService,
            MultiUniversityPageFetcherService pageFetcherService,
            UniversitySourcesAggregatorService aggregatorService,
            StudentService studentService,
            GeminiService geminiService
    ) {
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.aggregatorService = aggregatorService;
        this.studentService = studentService;
        this.geminiService = geminiService;
    }

    public UniversitySourcesAnalysisResponse analyse(Principal principal, UniversitySourcesAnalysisRequest request) {
        StudentProfile profile = studentService.getProfileEntity(principal);

        List<String> urls = (request.urls() == null || request.urls().isEmpty())
                ? registryService.getDefaultSources()
                : request.urls();
        urls = registryService.deduplicate(urls).stream().limit(10).toList();

        List<UniversitySourcePageResult> fetchedPages = pageFetcherService.fetchPages(urls);
        String combinedContext = aggregatorService.buildCombinedContext(fetchedPages, profile, request);

        return geminiService.getUniversitySourcesAdvice(request, profile, urls, fetchedPages, combinedContext);
    }

    public List<String> getDefaultSources() {
        return registryService.getDefaultSources();
    }
}
