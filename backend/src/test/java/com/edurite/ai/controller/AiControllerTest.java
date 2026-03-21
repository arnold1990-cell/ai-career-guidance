package com.edurite.ai.controller;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.StudentAiGuidanceService;
import com.edurite.ai.service.UniversitySourcesGuidanceService;
import com.edurite.ai.university.UniversitySourceCoverageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GeminiService geminiService;
    @MockBean
    private UniversitySourcesGuidanceService universitySourcesGuidanceService;
    @MockBean
    private UniversitySourceCoverageService sourceCoverageService;
    @MockBean
    private StudentAiGuidanceService studentAiGuidanceService;

    @Test
    @WithMockUser(username = "student@example.com", roles = "STUDENT")
    void analyseUniversitySourcesReturnsStructuredErrorBody() throws Exception {
        when(universitySourcesGuidanceService.analyse(any(), any(UniversitySourcesAnalysisRequest.class)))
                .thenThrow(new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                        "AI_PROVIDER_TIMEOUT",
                        "Gemini timed out",
                        "Could not connect to the AI service. Please try again shortly."));

        mockMvc.perform(post("/api/v1/ai/analyse-university-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UniversitySourcesAnalysisRequest(null, "Computer Science", "Software Developer", "Undergraduate", 5))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_TIMEOUT"))
                .andExpect(jsonPath("$.message").value("Could not connect to the AI service. Please try again shortly."));
    }
}
