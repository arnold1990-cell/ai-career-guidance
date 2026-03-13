package com.edurite.ai.service;

import com.edurite.ai.dto.AiGuidanceRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptBuilderTest {

    private final AiPromptBuilder promptBuilder = new AiPromptBuilder();

    @Test
    void buildsPromptWithStrictSections() {
        AiGuidanceRequest request = new AiGuidanceRequest(
                "Lebo", "Mokoena", "Technology", "Java, SQL", "Diploma", "Johannesburg",
                "Student", "Cloud engineering", "Math and science", "Public speaking"
        );

        String prompt = promptBuilder.buildGuidancePrompt(request);

        assertThat(prompt).contains("JSON format");
        assertThat(prompt).contains("suggestedCareers");
        assertThat(prompt).contains("Lebo");
    }
}
