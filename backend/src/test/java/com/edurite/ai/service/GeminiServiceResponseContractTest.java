package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiServiceResponseContractTest {

    @Test
    void fallbackResponseSetsAiLiveAndFallbackFlags() {
        GeminiService service = new GeminiService(new ObjectMapper(), new org.springframework.mock.env.MockEnvironment());
        ReflectionTestUtils.setField(service, "configuredApiKey", "");

        UniversitySourcesAnalysisResponse response = service.getUniversitySourcesAdvice(
                new UniversitySourcesAnalysisRequest(List.of("https://www.unisa.ac.za/page"), "Software", "Developer", "Undergraduate", 3),
                new StudentProfile(),
                List.of("https://www.unisa.ac.za/page"),
                List.of(new UniversitySourcePageResult("https://www.unisa.ac.za/page", "t", UniversityPageType.QUALIFICATION_LIST,
                        "content", Set.of("computer science"), true, null, null)),
                "context"
        );

        assertThat(response.aiLive()).isFalse();
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.warningMessage()).contains("Live AI guidance is temporarily unavailable");
    }
}

