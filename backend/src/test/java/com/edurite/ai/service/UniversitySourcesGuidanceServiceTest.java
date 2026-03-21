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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.edurite.ai.university.UniversityPageType.PROGRAMME_DETAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversitySourcesGuidanceServiceTest {

    @Mock private UniversitySourceRegistryService registryService;
    @Mock private PublicUniversitySourceDiscoveryService discoveryService;
    @Mock private MultiUniversityPageFetcherService pageFetcherService;
    @Mock private UniversitySourcesAggregatorService aggregatorService;
    @Mock private StudentService studentService;
    @Mock private GeminiService geminiService;
    @Mock private UniversityGuidanceResultEnricher resultEnricher;
    @Mock private UniversityStructuredRecommendationService structuredRecommendationService;

    private UniversitySourcesGuidanceService service;

    @BeforeEach
    void setUp() {
        service = new UniversitySourcesGuidanceService(
                registryService,
                discoveryService,
                pageFetcherService,
                aggregatorService,
                studentService,
                geminiService,
                resultEnricher,
                structuredRecommendationService
        );
    }

    @Test
    void analyseUsesAutomaticRetrievalWhenNoUrlsProvided() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software", "Undergraduate", 5);
        List<String> discoveredUrls = List.of("https://www.unisa.ac.za/a", "https://www.uj.ac.za/b");
        List<UniversitySourcePageResult> fetchedPages = List.of(
                new UniversitySourcePageResult(discoveredUrls.get(0), "A", PROGRAMME_DETAIL, "summary a", Set.of("software"), true, null, null)
        );

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(registryService.configuredUniversityCount()).thenReturn(12);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(24))).thenReturn(discoveredUrls);
        when(pageFetcherService.fetchPages(discoveredUrls)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(fetchedPages, profile, request)).thenReturn("context");
        UniversitySourcesAnalysisResponse baseResponse = new UniversitySourcesAnalysisResponse(true, false, "live Gemini", "FULLY_GROUNDED", 100, null, discoveredUrls, discoveredUrls, discoveredUrls, List.of(), 1, "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 60, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages), eq("context"))).thenReturn(baseResponse);
        when(resultEnricher.enrich(org.mockito.ArgumentMatchers.any(), eq(request), eq(profile), eq(discoveredUrls), eq(fetchedPages)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UniversitySourcesAnalysisResponse response = service.analyse(principal, request);

        verify(discoveryService).discoverSources(eq(profile), eq(request), eq(24));
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.diagnostics()).isNotNull();
    }

    @Test
    void analyseReturnsStructuredErrorWhenLiveAiFallsBackAndNoSourcesAreUsable() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(null, "Engineering", "Engineering", "Undergraduate", 5);

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(registryService.configuredUniversityCount()).thenReturn(12);
        when(discoveryService.discoverSources(eq(profile), eq(request), eq(24))).thenReturn(List.of());
        when(pageFetcherService.fetchPages(List.of())).thenReturn(List.of());
        when(aggregatorService.buildCombinedContext(List.of(), profile, request)).thenReturn("");
        UniversitySourcesAnalysisResponse fallbackResponse = new UniversitySourcesAnalysisResponse(false, true, "fallback recommendations", "NO_LIVE_SOURCES", 0, "warning", List.of(), List.of(), List.of(), List.of(), 0, "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0, "gemini");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(List.of()), eq(List.of()), eq(""))).thenReturn(fallbackResponse);
        when(structuredRecommendationService.buildResponse(eq(request), eq(profile), eq(List.of()), eq(List.of()), org.mockito.ArgumentMatchers.any(), eq("ERROR"), eq("UNAVAILABLE"), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> new UniversitySourcesAnalysisResponse("ERROR", false, true, "UNAVAILABLE", invocation.getArgument(7), "ERROR", 0, invocation.getArgument(7), List.of(), List.of(), List.of(), List.of(), 0, "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0, "registry-driven", null, List.of(), List.of(), List.of(), null, invocation.getArgument(4)));
        when(resultEnricher.enrich(org.mockito.ArgumentMatchers.any(), eq(request), eq(profile), eq(List.of()), eq(List.of()))).thenAnswer(invocation -> invocation.getArgument(0));

        UniversitySourcesAnalysisResponse response = service.analyse(principal, request);

        assertThat(response.status()).isEqualTo("ERROR");
        assertThat(response.mode()).isEqualTo("UNAVAILABLE");
    }
}
