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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.edurite.ai.university.UniversityPageType.PROGRAMME_DETAIL;
import static com.edurite.ai.university.UniversityPageType.QUALIFICATION_LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
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
    @Mock
    private UniversityGuidanceResultEnricher resultEnricher;

    @InjectMocks
    private UniversitySourcesGuidanceService service;

    @BeforeEach
    void setUp() {
        when(registryService.getActiveUniversities()).thenReturn(List.of());
        when(registryService.configuredUniversityCount()).thenReturn(0);
    }

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
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(20))).thenReturn(discoveredUrls);
        when(pageFetcherService.fetchPages(discoveredUrls)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(fetchedPages, profile, request)).thenReturn("context");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse(true, false, "SUCCESS", "LIVE", "FULLY_GROUNDED", 100, null, discoveredUrls, discoveredUrls, discoveredUrls, List.of(), 2, "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 60, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages), eq("context")))
                .thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages))).thenReturn(baseResponse);

        service.analyse(principal, request);

        verify(discoveryService).discoverSources(eq(profile), eq(request), eq(20));
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
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse(true, false, "SUCCESS", "LIVE", "FULLY_GROUNDED", 100, null, dedupedUrls, dedupedUrls, dedupedUrls, List.of(), 2, "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 50, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(dedupedUrls), eq(fetchedPages), eq("context")))
                .thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(dedupedUrls), eq(fetchedPages))).thenReturn(baseResponse);

        service.analyse(principal, request);

        verify(pageFetcherService).fetchPages(dedupedUrls);
    }

    @Test
    void analyseCapsDiscoveryLimitForLargeUniversityRegistry() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(registryService.configuredUniversityCount()).thenReturn(55);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(20))).thenReturn(List.of());
        when(registryService.getFallbackSources(20)).thenReturn(List.of());
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse(true, false, "SUCCESS", "LIVE", "NO_LIVE_SOURCES", 0, null, List.of(), List.of(), List.of(), List.of(), 0,
                "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 55, "gemini");
        when(pageFetcherService.fetchPages(List.of())).thenReturn(List.of());
        when(aggregatorService.buildCombinedContext(List.of(), profile, request)).thenReturn("");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(List.of()), eq(List.of()), eq(""))).thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(List.of()), eq(List.of()))).thenReturn(baseResponse);

        service.analyse(principal, request);

        verify(discoveryService).discoverSources(eq(profile), eq(request), eq(20));
    }


    @Test
    void analyseSynthesizesTerminalFailureWhenFetcherMissesARequestedSource() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);
        List<String> discoveredUrls = List.of("https://www.unisa.ac.za/a", "https://www.uj.ac.za/b");
        List<UniversitySourcePageResult> fetchedPages = List.of(
                new UniversitySourcePageResult(discoveredUrls.get(0), "A", PROGRAMME_DETAIL, "summary a", Set.of("software"), true, null, null)
        );

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(20))).thenReturn(discoveredUrls);
        when(pageFetcherService.fetchPages(discoveredUrls)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(org.mockito.ArgumentMatchers.anyList(), eq(profile), eq(request))).thenReturn("context");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse(true, false, "SUCCESS", "LIVE", "PARTIALLY_GROUNDED", 50, null, discoveredUrls, discoveredUrls, List.of(discoveredUrls.get(0)), List.of(discoveredUrls.get(1)), 1,
                "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 60, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(discoveredUrls), org.mockito.ArgumentMatchers.anyList(), eq("context"))).thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(discoveredUrls), org.mockito.ArgumentMatchers.anyList())).thenReturn(baseResponse);

        UniversitySourcesAnalysisResponse response = service.analyse(principal, request);

        assertThat(response.failedUrls()).contains(discoveredUrls.get(1));
        assertThat(response.sourceDiagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.sourceUrl()).isEqualTo(discoveredUrls.get(1));
            assertThat(diagnostic.failureReason()).contains("terminal failure was synthesized");
        });
    }

    @Test
    void analyseStillCallsGeminiWhenSourcePipelineIsEmpty() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(20))).thenReturn(List.of());
        when(registryService.getFallbackSources(20)).thenReturn(List.of());
        when(pageFetcherService.fetchPages(List.of())).thenReturn(List.of());
        when(aggregatorService.buildCombinedContext(List.of(), profile, request)).thenReturn("");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse(true, false, "SUCCESS", "LIVE", "NO_LIVE_SOURCES", 0, null, List.of(), List.of(), List.of(), List.of(), 0,
                        "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 55, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(List.of()), eq(List.of()), eq("")))
                .thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(List.of()), eq(List.of()))).thenReturn(baseResponse);

        service.analyse(principal, request);

        verify(geminiService).getUniversitySourcesAdvice(eq(request), eq(profile), eq(List.of()), eq(List.of()), eq(""));
    }

    @Test
    void analyseFallsBackToRegistrySourcesWhenDiscoveryReturnsNothing() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);
        List<String> fallbackUrls = List.of("https://www.uct.ac.za/", "https://www.wits.ac.za/");

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(registryService.configuredUniversityCount()).thenReturn(0);
        when(registryService.getActiveUniversities()).thenReturn(List.of());
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(20))).thenReturn(List.of());
        when(registryService.getFallbackSources(20)).thenReturn(fallbackUrls);
        when(pageFetcherService.fetchPages(fallbackUrls)).thenReturn(List.of());
        when(aggregatorService.buildCombinedContext(List.of(), profile, request)).thenReturn("");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse(false, true, "ERROR", "UNAVAILABLE", "NO_LIVE_SOURCES", 0, null, fallbackUrls, fallbackUrls, List.of(), fallbackUrls, 0,
                "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 55, "gemini");
        when(geminiService.getUniversitySourcesAdvice(
                eq(request),
                eq(profile),
                eq(fallbackUrls),
                argThat(results -> results.size() == 2
                        && results.stream().allMatch(result -> !result.success())
                        && results.stream().allMatch(result -> result.failureType() == com.edurite.ai.university.UniversityCrawlFailureType.FETCH_ERROR)),
                eq("")))
                .thenReturn(baseResponse);
        when(resultEnricher.enrich(
                eq(baseResponse),
                eq(request),
                eq(profile),
                eq(fallbackUrls),
                anyList()))
                .thenReturn(baseResponse);

        UniversitySourcesAnalysisResponse response = service.analyse(principal, request);

        assertThat(response.requestedSources()).containsExactlyElementsOf(fallbackUrls);
        assertThat(response.failedUrls()).containsExactlyElementsOf(fallbackUrls);
        assertThat(response.sourceDiagnostics()).hasSize(2);
    }

    @Test
    void analyseMarksResponsePartialWhenSomeSourcesSucceed() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);
        List<String> discoveredUrls = List.of("https://www.unisa.ac.za/a", "https://www.uj.ac.za/b");
        List<UniversitySourcePageResult> fetchedPages = List.of(
                new UniversitySourcePageResult(discoveredUrls.get(0), "A", PROGRAMME_DETAIL, "summary a", Set.of("software"), true, null, null),
                new UniversitySourcePageResult(discoveredUrls.get(1), "B", QUALIFICATION_LIST, "summary b", Set.of("computing"), false, "timeout", com.edurite.ai.university.UniversityCrawlFailureType.TIMEOUT)
        );

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(20))).thenReturn(discoveredUrls);
        when(registryService.configuredUniversityCount()).thenReturn(0);
        when(registryService.getActiveUniversities()).thenReturn(List.of());
        when(pageFetcherService.fetchPages(discoveredUrls)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(fetchedPages, profile, request)).thenReturn("context");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse(true, false, "SUCCESS", "LIVE", "PARTIALLY_GROUNDED", 50, null, discoveredUrls, discoveredUrls, List.of(discoveredUrls.get(0)), List.of(discoveredUrls.get(1)), 1,
                "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 60, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages), eq("context"))).thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages))).thenReturn(baseResponse);

        UniversitySourcesAnalysisResponse response = service.analyse(principal, request);

        assertThat(response.mode()).isEqualTo("PARTIAL");
        assertThat(response.totalSourcesUsed()).isEqualTo(1);
        assertThat(response.failedUrls()).containsExactly(discoveredUrls.get(1));
    }

}
