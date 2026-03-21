package com.edurite.ai.controller;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.StudentAiGuidanceService;
import com.edurite.ai.service.UniversitySourcesGuidanceService;
import com.edurite.ai.university.UniversitySourceCoverageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiControllerUniversityAnalysisTest {

    @Test
    void returnsStructuredErrorPayloadInsteadOfEmptySuccessFallback() throws Exception {
        GeminiService geminiService = mock(GeminiService.class);
        UniversitySourcesGuidanceService guidanceService = mock(UniversitySourcesGuidanceService.class);
        UniversitySourceCoverageService coverageService = mock(UniversitySourceCoverageService.class);
        StudentAiGuidanceService studentAiGuidanceService = mock(StudentAiGuidanceService.class);
        AiController controller = new AiController(geminiService, guidanceService, coverageService, studentAiGuidanceService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(guidanceService.analyse(any(), any())).thenReturn(new UniversitySourcesAnalysisResponse(
                "ERROR", false, true, "UNAVAILABLE", "AI Guidance unavailable", "ERROR", 0, "AI Guidance unavailable", List.of(), List.of(), List.of(), List.of(), 0,
                "summary", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0, "registry-driven", null, List.of(), List.of(), List.of(), null, null
        ));

        mockMvc.perform(post("/api/v1/ai/analyse-university-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new UniversitySourcesAnalysisRequest(List.of(), "Engineering", "Engineering", "Undergraduate", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.mode").value("UNAVAILABLE"));
    }
}
