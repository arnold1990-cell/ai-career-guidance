package com.edurite.ai.controller;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.UniversitySourceRegistryService;
import com.edurite.ai.service.UniversitySourcesAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiControllerTest {

    @Test
    void defaultSourcesEndpointReturnsRegistryValues() {
        GeminiService geminiService = mock(GeminiService.class);
        UniversitySourcesAnalysisService analysisService = mock(UniversitySourcesAnalysisService.class);
        UniversitySourceRegistryService registryService = mock(UniversitySourceRegistryService.class);
        when(registryService.defaultSources()).thenReturn(List.of("https://www.unisa.ac.za/default"));

        AiController controller = new AiController(geminiService, analysisService, registryService);
        var response = controller.defaultUniversitySources();

        assertThat(response.getBody()).containsExactly("https://www.unisa.ac.za/default");
    }

    @Test
    void analyseEndpointReturnsServicePayload() {
        GeminiService geminiService = mock(GeminiService.class);
        UniversitySourcesAnalysisService analysisService = mock(UniversitySourcesAnalysisService.class);
        UniversitySourceRegistryService registryService = mock(UniversitySourceRegistryService.class);

        when(analysisService.analyse(any(), any())).thenReturn(new UniversitySourcesAnalysisResponse(
                List.of("https://www.unisa.ac.za/x"), List.of("https://www.unisa.ac.za/x"), List.of(), "summary",
                List.of("Software Developer"), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 1, 70, "gemini-2.5-flash"
        ));

        AiController controller = new AiController(geminiService, analysisService, registryService);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRequestURI()).thenReturn("/api/v1/ai/analyse-university-sources");

        var response = controller.analyseUniversitySources(
                new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/x"), null, null, null, 10),
                (Principal) () -> "student@example.com",
                servletRequest
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(((UniversitySourcesAnalysisResponse) response.getBody()).summary()).isEqualTo("summary");
    }
}
