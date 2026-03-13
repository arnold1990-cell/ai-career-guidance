package com.edurite.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiContextStartupSafetyTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class)
            .withBean(GeminiService.class)
            .withPropertyValues(
                    "spring.profiles.active=test",
                    "gemini.api-key=",
                    "gemini.model=   "
            );

    @Test
    void geminiBeanStartsEvenWhenConfigIsBlankUnderTestProfile() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GeminiService.class);
            assertThat(context).hasNotFailed();
        });
    }
}
