package com.edurite.ai.service;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiServiceConfigSafetyTest {

    @Test
    void missingApiKeyFailsAtRequestTimeNotConstructionTime() {
        GeminiService service = new GeminiService(new ObjectMapper(), new MockEnvironment());
        ReflectionTestUtils.setField(service, "GEMINI_API_KEY", "   ");

        assertThatThrownBy(() -> service.getCareerAdvice(
                new CareerAdviceRequest("hs", "tech", "java", "harare")))
                .isInstanceOf(AiServiceException.class)
                .satisfies(ex -> {
                    AiServiceException aiEx = (AiServiceException) ex;
                    assertThat(aiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(aiEx.getMessage()).contains("API key is not configured");
                });
    }

    @Test
    void missingApiKeyReturnsFallbackForUniversitySourceAnalysis() {
        GeminiService service = new GeminiService(new ObjectMapper(), new MockEnvironment());
        ReflectionTestUtils.setField(service, "GEMINI_API_KEY", "");

        StudentProfile profile = new StudentProfile();

        UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                new UniversitySourcesAnalysisRequest(
                        List.of("https://www.unisa.ac.za/page"),
                        "Software",
                        "Developer",
                        "Undergraduate",
                        10
                ),
                profile,
                List.of("https://www.unisa.ac.za/page"),
                List.of(new UniversitySourcePageResult(
                        "https://www.unisa.ac.za/page",
                        "t",
                        UniversityPageType.QUALIFICATION_LIST,
                        "content",
                        Set.of("computer science"),
                        true,
                        null,
                        null
                )),
                "content"
        );

        assertThat(response.summary()).contains("Based on the available university sources");
        assertThat(response.recommendedCareers()).isNotEmpty();
        assertThat(response.warnings()).isNotEmpty();
        assertThat(response.fallbackUsed()).isTrue();
    }

    @Test
    void resolveModelReturnsHardcodedModel() {
        GeminiService service = new GeminiService(new ObjectMapper(), new MockEnvironment());

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveModel");

        assertThat(resolved).isEqualTo("gemini-2.5-flash");
    }

    @Test
    void resolveBaseUrlReturnsNormalizedHardcodedBaseUrl() {
        GeminiService service = new GeminiService(new ObjectMapper(), new MockEnvironment());

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveBaseUrl");

        assertThat(resolved).isEqualTo("https://generativelanguage.googleapis.com");
    }

    @Test
    void healthCheckUsesNormalizedModelPath() {
        GeminiService service = new GeminiService(new ObjectMapper(), new MockEnvironment());

        GeminiService.GeminiHealthCheck health = service.checkHealth();

        assertThat(health.endpoint()).contains("/models/");
    }
}