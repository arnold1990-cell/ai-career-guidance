package com.edurite.ai.service;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.exception.AiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiServiceConfigSafetyTest {

    @Test
    void missingApiKeyFailsAtRequestTimeNotConstructionTime() {
        GeminiService service = new GeminiService(new ObjectMapper());
        ReflectionTestUtils.setField(service, "apiKey", "   ");
        ReflectionTestUtils.setField(service, "model", "models/gemini-2.0-flash");

        assertThatThrownBy(() -> service.getCareerAdvice(new CareerAdviceRequest("hs", "tech", "java", "harare")))
                .isInstanceOf(AiServiceException.class)
                .satisfies(ex -> {
                    AiServiceException aiEx = (AiServiceException) ex;
                    assertThat(aiEx.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(aiEx.getMessage()).contains("API key is not configured");
                });
    }
}
