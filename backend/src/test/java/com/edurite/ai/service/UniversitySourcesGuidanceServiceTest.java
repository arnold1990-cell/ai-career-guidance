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
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(24))).thenReturn(discoveredUrls);
        when(pageFetcherService.fetchPages(discoveredUrls)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(fetchedPages, profile, request)).thenReturn("context");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse("SUCCESS", true, false, "LIVE", "EduRite analysed live university sources successfully.", "FULLY_GROUNDED", 100, null, discoveredUrls, discoveredUrls, discoveredUrls, List.of(), 2, "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 60, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages), eq("context")))
                .thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages))).thenReturn(baseResponse);

        service.analyse(principal, request);

        verify(discoveryService).discoverSources(eq(profile), eq(request), eq(24));
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
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse("SUCCESS", true, false, "LIVE", "EduRite analysed live university sources successfully.", "FULLY_GROUNDED", 100, null, dedupedUrls, dedupedUrls, dedupedUrls, List.of(), 2, "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 50, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(dedupedUrls), eq(fetchedPages), eq("context")))
                .thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(dedupedUrls), eq(fetchedPages))).thenReturn(baseResponse);

        service.analyse(principal, request);

        verify(pageFetcherService).fetchPages(dedupedUrls);
    }

    @Test
    void analyseScalesDiscoveryLimitForLargeUniversityRegistry() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(registryService.configuredUniversityCount()).thenReturn(55);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(110))).thenReturn(List.of());
        when(registryService.getFallbackSources(110)).thenReturn(List.of());
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse("PARTIAL", true, false, "UNAVAILABLE", "EduRite could not analyse live university sources for this request.", "NO_LIVE_SOURCES", 0, null, List.of(), List.of(), List.of(), List.of(), 0,
                "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 55, "gemini");
        when(pageFetcherService.fetchPages(List.of())).thenReturn(List.of());
        when(aggregatorService.buildCombinedContext(List.of(), profile, request)).thenReturn("");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(List.of()), eq(List.of()), eq(""))).thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(List.of()), eq(List.of()))).thenReturn(baseResponse);

        service.analyse(principal, request);

        verify(discoveryService).discoverSources(eq(profile), eq(request), eq(110));
    }

    @Test
    void analyseStillCallsGeminiWhenSourcePipelineIsEmpty() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(24))).thenReturn(List.of());
        when(registryService.getFallbackSources(24)).thenReturn(List.of());
        when(pageFetcherService.fetchPages(List.of())).thenReturn(List.of());
        when(aggregatorService.buildCombinedContext(List.of(), profile, request)).thenReturn("");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse("PARTIAL", true, false, "UNAVAILABLE", "EduRite could not analyse live university sources for this request.", "NO_LIVE_SOURCES", 0, null, List.of(), List.of(), List.of(), List.of(), 0,
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
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(24))).thenReturn(List.of());
        when(registryService.getFallbackSources(24)).thenReturn(fallbackUrls);
        when(pageFetcherService.fetchPages(fallbackUrls)).thenReturn(List.of());
        when(aggregatorService.buildCombinedContext(List.of(), profile, request)).thenReturn("");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse("PARTIAL", false, true, "PARTIAL", "Live Gemini guidance was unavailable, so EduRite returned resilient profile-based recommendations.", "NO_LIVE_SOURCES", 0, null, fallbackUrls, fallbackUrls, List.of(), fallbackUrls, 0,
                "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 55, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(fallbackUrls), eq(List.of()), eq(""))).thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(fallbackUrls), eq(List.of()))).thenReturn(baseResponse);

        UniversitySourcesAnalysisResponse response = service.analyse(principal, request);

        assertThat(response.requestedSources()).containsExactlyElementsOf(fallbackUrls);
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
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(24))).thenReturn(discoveredUrls);
        when(registryService.configuredUniversityCount()).thenReturn(0);
        when(registryService.getActiveUniversities()).thenReturn(List.of());
        when(pageFetcherService.fetchPages(discoveredUrls)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(fetchedPages, profile, request)).thenReturn("context");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse("PARTIAL", true, false, "PARTIAL", "EduRite returned partial guidance using the university sources that completed in time.", "PARTIALLY_GROUNDED", 50, null, discoveredUrls, discoveredUrls, List.of(discoveredUrls.get(0)), List.of(discoveredUrls.get(1)), 1,
                "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 60, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages), eq("context"))).thenReturn(baseResponse);
        when(resultEnricher.enrich(eq(baseResponse), eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages))).thenReturn(baseResponse);

        UniversitySourcesAnalysisResponse response = service.analyse(principal, request);

        assertThat(response.mode()).isEqualTo("PARTIAL");
        assertThat(response.totalSourcesUsed()).isEqualTo(1);
        assertThat(response.failedUrls()).containsExactly(discoveredUrls.get(1));
    }

}
