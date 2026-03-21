package com.edurite.ai.config;

import com.edurite.ai.service.GeminiModelResolver;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class GeminiConfigurationValidator {

    private static final Logger log = LoggerFactory.getLogger(GeminiConfigurationValidator.class);

    private final GeminiProperties geminiProperties;
    private final Environment environment;

    public GeminiConfigurationValidator(GeminiProperties geminiProperties, Environment environment) {
        this.geminiProperties = geminiProperties;
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        String apiKey = trim(geminiProperties.getApiKey());
        String model = GeminiModelResolver.resolveModelName(trim(geminiProperties.getModel()));
        String baseUrl = GeminiModelResolver.normalizeBaseUrl(trim(geminiProperties.getBaseUrl()));
        boolean apiKeyPresent = !apiKey.isBlank();

        log.info("Gemini API key present: {}", apiKeyPresent);
        log.info("Gemini model: {}", model);
        log.info("Gemini base URL: {}", baseUrl);

        boolean testProfileActive = environment.matchesProfiles("test");
        if (!geminiProperties.isFailFast() || testProfileActive) {
            return;
        }

        if (!apiKeyPresent) {
            throw new IllegalStateException("Gemini configuration is invalid: GEMINI_API_KEY (or gemini.api-key) is missing.");
        }
        if (model.isBlank()) {
            throw new IllegalStateException("Gemini configuration is invalid: GEMINI_MODEL (or gemini.model) is missing.");
        }
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("Gemini configuration is invalid: GEMINI_BASE_URL (or gemini.base-url) is missing.");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
