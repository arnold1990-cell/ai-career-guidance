package com.edurite.ai.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// @ConfigurationProperties maps ai.gemini.* values from application configuration.
@ConfigurationProperties(prefix = "ai.gemini")
@Validated
/**
 * Gemini runtime settings loaded from environment variables or property files.
 *
 * Keep this class focused on configuration only so business logic remains easy to test.
 */
public record GeminiProperties(
        boolean enabled,
        String apiKey,
        @NotBlank String model,
        @Min(1) int timeoutSeconds
) {
}
