package com.edurite.ai.service;

import com.edurite.ai.dto.AiGuidanceRequest;
import com.edurite.ai.dto.AiGuidanceResponse;
import com.edurite.ai.provider.AiTextProvider;
import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.service.RecommendationService;
import com.edurite.student.service.StudentService;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiGuidanceServiceImplTest {

    private final AiPromptBuilder promptBuilder = new AiPromptBuilder();
    private final AiTextProvider aiTextProvider = mock(AiTextProvider.class);
    private final RecommendationService recommendationService = mock(RecommendationService.class);
    private final StudentService studentService = mock(StudentService.class);

    private final AiGuidanceServiceImpl service = new AiGuidanceServiceImpl(
            promptBuilder,
            aiTextProvider,
            recommendationService,
            studentService
    );

    @Test
    void fallsBackWhenProviderUnavailable() {
        when(aiTextProvider.isAvailable()).thenReturn(false);
        when(recommendationService.generateForStudent(org.mockito.ArgumentMatchers.any())).thenReturn(
                new RecommendationResultDto(List.of(), List.of(), List.of(), List.of("Tip"), "rule-engine-v3")
        );

        Principal principal = () -> "student@edurite.local";
        AiGuidanceResponse response = service.generateGuidance(
                new AiGuidanceRequest("Lebo", null, null, null, null, null, null, null, null, null),
                principal
        );

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.source()).isEqualTo("rule-engine-v3");
    }
}
