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
        GeminiService service = new GeminiService(new ObjectMapper(), new org.springframework.mock.env.MockEnvironment());
        ReflectionTestUtils.setField(service, "configuredApiKey", "   ");
        ReflectionTestUtils.setField(service, "model", "models/gemini-2.0-flash");

        assertThatThrownBy(() -> service.getCareerAdvice(new CareerAdviceRequest("hs", "tech", "java", "harare")))
                .isInstanceOf(AiServiceException.class)
                .satisfies(ex -> {
                    AiServiceException aiEx = (AiServiceException) ex;
                    assertThat(aiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(aiEx.getMessage()).contains("API key is not configured");
                });
    }

    @Test
    void missingApiKeyReturnsFallbackForUniversitySourceAnalysis() {
        GeminiService service = new GeminiService(new ObjectMapper(), new org.springframework.mock.env.MockEnvironment());
        ReflectionTestUtils.setField(service, "configuredApiKey", "");
        ReflectionTestUtils.setField(service, "model", "gemini-2.5-flash");

        StudentProfile profile = new StudentProfile();
        UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/page"), "Software", "Developer", "Undergraduate", 10),
                profile,
                List.of("https://www.unisa.ac.za/page"),
                List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/page", "t", UniversityPageType.QUALIFICATION_LIST,
                        "content", Set.of("computer science"), true, null, null)),
                "content"
        );

        assertThat(response.summary()).contains("Based on the available university sources");
        assertThat(response.recommendedCareers()).isNotEmpty();
        assertThat(response.warnings()).isNotEmpty();
    }

    @Test
    void acceptsDotNotationGeminiApiKeyProperty() {
        org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment()
                .withProperty("gemini.api.key", "dot-notation-key");
        GeminiService service = new GeminiService(new ObjectMapper(), environment);

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveApiKey");

        assertThat(resolved).isEqualTo("dot-notation-key");
    }

    @Test
    void resolvesModelFromEnvironmentWhenValueFieldIsBlank() {
        org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment()
                .withProperty("GEMINI_MODEL", "models/gemini-2.0-flash");
        GeminiService service = new GeminiService(new ObjectMapper(), environment);
        ReflectionTestUtils.setField(service, "model", "  ");

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveModel");

        assertThat(resolved).isEqualTo("gemini-2.0-flash");
    }

    @Test
    void resolvesBaseUrlFromEnvironmentWhenValueFieldIsBlank() {
        org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment()
                .withProperty("GEMINI_BASE_URL", "https://example.googleapis.com/");
        GeminiService service = new GeminiService(new ObjectMapper(), environment);
        ReflectionTestUtils.setField(service, "baseUrl", "   ");

        String resolved = (String) ReflectionTestUtils.invokeMethod(service, "resolveBaseUrl");

        assertThat(resolved).isEqualTo("https://example.googleapis.com");
    }

}
