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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.edurite.ai.university.UniversityPageType.PROGRAMME_DETAIL;
import static com.edurite.ai.university.UniversityPageType.QUALIFICATION_LIST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversitySourcesGuidanceServiceTest {

    @Mock
    private UniversitySourceRegistryService registryService;
    @Mock
    private PublicUniversitySourceDiscoveryService discoveryService;
    @Mock
    private MultiUniversityPageFetcherService pageFetcherService;
    @Mock
    private UniversitySourcesAggregatorService aggregatorService;
    @Mock
    private StudentService studentService;
    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private UniversitySourcesGuidanceService service;

    @Test
    void analyseUsesAutomaticRetrievalWhenNoUrlsProvided() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);
        List<String> discoveredUrls = List.of("https://www.unisa.ac.za/a", "https://www.uj.ac.za/b");
        List<UniversitySourcePageResult> fetchedPages = List.of(
                new UniversitySourcePageResult(discoveredUrls.get(0), "A", PROGRAMME_DETAIL, "summary a", Set.of("software"), true, null, null),
                new UniversitySourcePageResult(discoveredUrls.get(1), "B", QUALIFICATION_LIST, "summary b", Set.of("computing"), true, null, null)
        );

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(18))).thenReturn(discoveredUrls);
        when(pageFetcherService.fetchPages(discoveredUrls)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(fetchedPages, profile, request)).thenReturn("context");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages), eq("context")))
                .thenReturn(new UniversitySourcesAnalysisResponse(true, false, null, discoveredUrls, discoveredUrls, List.of(), 2, "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 60, "gemini"));

        service.analyse(principal, request);

        verify(discoveryService).discoverSources(eq(profile), eq(request), eq(18));
        verify(geminiService).getUniversitySourcesAdvice(eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages), eq("context"));
    }

    @Test
    void analyseProcessesProvidedUrls() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(
                List.of("https://www.unisa.ac.za/programmes/a", "https://www.uj.ac.za/programmes/b"),
                "Computer Science",
                "Software Developer",
                "Undergraduate",
                5
        );

        List<String> dedupedUrls = List.of("https://www.unisa.ac.za/programmes/a", "https://www.uj.ac.za/programmes/b");
        List<UniversitySourcePageResult> fetchedPages = List.of(
                new UniversitySourcePageResult(dedupedUrls.get(0), "A", PROGRAMME_DETAIL, "a content", Set.of("software"), true, null, null),
                new UniversitySourcePageResult(dedupedUrls.get(1), "B", QUALIFICATION_LIST, "b content", Set.of("computing"), true, null, null)
        );

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(registryService.deduplicate(request.urls())).thenReturn(dedupedUrls);
        when(pageFetcherService.fetchPages(dedupedUrls)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(fetchedPages, profile, request)).thenReturn("context");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(dedupedUrls), eq(fetchedPages), eq("context")))
                .thenReturn(new UniversitySourcesAnalysisResponse(true, false, null, dedupedUrls, dedupedUrls, List.of(), 2, "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 50, "gemini"));

        service.analyse(principal, request);

        verify(pageFetcherService).fetchPages(dedupedUrls);
    }

    @Test
    void analyseStillCallsGeminiWhenSourcePipelineIsEmpty() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(18))).thenReturn(List.of());
        when(pageFetcherService.fetchPages(List.of())).thenReturn(List.of());
        when(aggregatorService.buildCombinedContext(List.of(), profile, request)).thenReturn("");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(List.of()), eq(List.of()), eq("")))
                .thenReturn(new UniversitySourcesAnalysisResponse(true, false, null, List.of(), List.of(), List.of(), 0,
                        "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 55, "gemini"));

        service.analyse(principal, request);

        verify(geminiService).getUniversitySourcesAdvice(eq(request), eq(profile), eq(List.of()), eq(List.of()), eq(""));
    }
}
