package com.edurite.ai.service;

import com.edurite.ai.dto.FetchedUniversityPage;
import com.edurite.ai.dto.UniversityPageType;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UniversitySourcesAnalysisServiceTest {

    @Test
    void fallsBackWhenGeminiFails() {
        UniversitySourceRegistryService registry = new UniversitySourceRegistryService();
        MultiUniversityPageFetcherService fetcher = mock(MultiUniversityPageFetcherService.class);
        UniversitySourcesAggregatorService aggregator = new UniversitySourcesAggregatorService();
        UniversityGuidancePromptBuilder promptBuilder = new UniversityGuidancePromptBuilder();
        GeminiService geminiService = mock(GeminiService.class);
        StudentService studentService = mock(StudentService.class);

        StudentProfile profile = new StudentProfile();
        profile.setQualificationLevel("Grade 12");
        profile.setInterests("Technology");
        when(studentService.getProfileEntity(any())).thenReturn(profile);
        when(fetcher.fetchPages(any())).thenReturn(List.of(
                new FetchedUniversityPage("https://www.unisa.ac.za/a", "Undergraduate programmes", UniversityPageType.QUALIFICATION_LIST,
                        "Computer Science and Information Systems", List.of("Computer Science"), List.of(), true, null)
        ));
        when(geminiService.generateJsonNode(any())).thenThrow(new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "down"));
        when(geminiService.resolvedModelName()).thenReturn("gemini-2.5-flash");

        UniversitySourcesAnalysisService service = new UniversitySourcesAnalysisService(registry, fetcher, aggregator, promptBuilder, geminiService, studentService);
        var response = service.analyse(new UniversitySourcesAnalysisRequest(null, null, null, null, 10), (Principal) () -> "student@example.com");

        assertThat(response.summary()).contains("Fallback guidance");
        assertThat(response.warnings()).anyMatch(w -> w.contains("fallback"));
        assertThat(response.recommendedCareers()).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void parsesStructuredGeminiJsonWhenAvailable() throws Exception {
        UniversitySourceRegistryService registry = new UniversitySourceRegistryService();
        MultiUniversityPageFetcherService fetcher = mock(MultiUniversityPageFetcherService.class);
        UniversitySourcesAggregatorService aggregator = new UniversitySourcesAggregatorService();
        UniversityGuidancePromptBuilder promptBuilder = new UniversityGuidancePromptBuilder();
        GeminiService geminiService = mock(GeminiService.class);
        StudentService studentService = mock(StudentService.class);

        StudentProfile profile = new StudentProfile();
        when(studentService.getProfileEntity(any())).thenReturn(profile);
        when(fetcher.fetchPages(any())).thenReturn(List.of(
                new FetchedUniversityPage("https://www.unisa.ac.za/a", "Undergraduate programmes", UniversityPageType.QUALIFICATION_LIST,
                        "Computer Science", List.of("Computer Science"), List.of(), true, null)
        ));
        String json = """
                {"summary":"Good fit","recommendedCareers":["Data Analyst"],"recommendedProgrammes":["BSc Computer Science"],"recommendedUniversities":["UNISA"],"keyRequirements":["Math"],"skillGaps":["Portfolio"],"recommendedNextSteps":["Apply"],"warnings":[],"suitabilityScore":88}
                """;
        when(geminiService.generateJsonNode(any())).thenReturn(new ObjectMapper().readTree(json));
        when(geminiService.resolvedModelName()).thenReturn("gemini-2.5-flash");

        UniversitySourcesAnalysisService service = new UniversitySourcesAnalysisService(registry, fetcher, aggregator, promptBuilder, geminiService, studentService);
        var response = service.analyse(new UniversitySourcesAnalysisRequest(null, null, null, null, 10), (Principal) () -> "student@example.com");

        assertThat(response.summary()).isEqualTo("Good fit");
        assertThat(response.recommendedCareers()).contains("Data Analyst");
        assertThat(response.suitabilityScore()).isEqualTo(88);
    }
}
