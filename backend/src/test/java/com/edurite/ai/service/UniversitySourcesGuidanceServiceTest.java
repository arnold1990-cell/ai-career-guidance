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
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.edurite.ai.university.UniversityPageType.PROGRAMME_DETAIL;
import static com.edurite.ai.university.UniversityPageType.QUALIFICATION_LIST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversitySourcesGuidanceServiceTest {

    @Mock
    private UniversitySourceRegistryService registryService;
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
    void analyseProcessesAllProvidedUrlsAndPassesAggregatedResultsToGemini() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(
                List.of(
                        "https://www.unisa.ac.za/programmes/a",
                        "https://www.uj.ac.za/programmes/b",
                        "https://www.wits.ac.za/programmes/c"
                ),
                "Computer Science",
                "Software Developer",
                "Undergraduate",
                5
        );

        List<String> dedupedUrls = List.of(
                "https://www.unisa.ac.za/programmes/a",
                "https://www.uj.ac.za/programmes/b",
                "https://www.wits.ac.za/programmes/c"
        );
        List<UniversitySourcePageResult> fetchedPages = List.of(
                new UniversitySourcePageResult(dedupedUrls.get(0), "A", PROGRAMME_DETAIL, "a content", Set.of("software"), true, null),
                new UniversitySourcePageResult(dedupedUrls.get(1), "B", QUALIFICATION_LIST, "b content", Set.of("computing"), true, null),
                new UniversitySourcePageResult(dedupedUrls.get(2), "C", QUALIFICATION_LIST, "c content", Set.of(), false, "Failed to fetch page")
        );
        UniversitySourcesAnalysisResponse geminiResponse = new UniversitySourcesAnalysisResponse(
                dedupedUrls,
                List.of(dedupedUrls.get(0), dedupedUrls.get(1)),
                List.of(dedupedUrls.get(2)),
                2,
                "summary",
                List.of("Software Developer"),
                List.of("BSc Computer Science"),
                List.of("UNISA", "University of Johannesburg"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                80,
                "gemini-2.5-flash"
        );

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(registryService.deduplicate(request.urls())).thenReturn(dedupedUrls);
        when(pageFetcherService.fetchPages(dedupedUrls)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(fetchedPages, profile, request))
                .thenReturn("merged context from two successful pages");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(dedupedUrls), eq(fetchedPages), any()))
                .thenReturn(geminiResponse);

        UniversitySourcesAnalysisResponse response = service.analyse(principal, request);

        assertThat(response.totalSourcesUsed()).isEqualTo(2);
        assertThat(response.successfullyAnalysedUrls()).containsExactly(dedupedUrls.get(0), dedupedUrls.get(1));
        assertThat(response.failedUrls()).containsExactly(dedupedUrls.get(2));

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).getUniversitySourcesAdvice(eq(request), eq(profile), eq(dedupedUrls), eq(fetchedPages), contextCaptor.capture());
        assertThat(contextCaptor.getValue()).contains("merged context from two successful pages");

        verify(pageFetcherService).fetchPages(dedupedUrls);
    }

    @Test
    void analyseUsesAllDefaultSourcesWhenRequestUrlsMissing() {
        Principal principal = () -> "student";
        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisRequest request = new UniversitySourcesAnalysisRequest(
                null,
                "Computer Science",
                "Software Developer",
                "Undergraduate",
                5
        );

        List<String> defaultSources = List.of(
                "https://www.unisa.ac.za/default-1",
                "https://www.uj.ac.za/default-2"
        );
        List<UniversitySourcePageResult> fetchedPages = List.of(
                new UniversitySourcePageResult(defaultSources.get(0), "A", PROGRAMME_DETAIL, "a content", Set.of("software"), true, null),
                new UniversitySourcePageResult(defaultSources.get(1), "B", QUALIFICATION_LIST, "b content", Set.of("computing"), true, null)
        );

        when(studentService.getProfileEntity(principal)).thenReturn(profile);
        when(registryService.getDefaultSources()).thenReturn(defaultSources);
        when(registryService.deduplicate(defaultSources)).thenReturn(defaultSources);
        when(pageFetcherService.fetchPages(defaultSources)).thenReturn(fetchedPages);
        when(aggregatorService.buildCombinedContext(fetchedPages, profile, request)).thenReturn("context");
        when(geminiService.getUniversitySourcesAdvice(eq(request), eq(profile), eq(defaultSources), eq(fetchedPages), eq("context")))
                .thenReturn(new UniversitySourcesAnalysisResponse(
                        defaultSources,
                        defaultSources,
                        List.of(),
                        2,
                        "summary",
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        50,
                        "gemini-2.5-flash"
                ));

        service.analyse(principal, request);

        verify(pageFetcherService).fetchPages(defaultSources);
        verify(geminiService).getUniversitySourcesAdvice(eq(request), eq(profile), eq(defaultSources), eq(fetchedPages), eq("context"));
    }
}
