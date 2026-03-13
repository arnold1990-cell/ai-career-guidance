package com.edurite.ai.provider;

import com.edurite.ai.config.GeminiProperties;
import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.types.GenerateContentResponse;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
/**
 * Google Gemini implementation for text generation.
 *
 * It only exposes clean text and keeps all SDK details internal to this class.
 */
public class GeminiTextProvider implements AiTextProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiTextProvider.class);

    private final GeminiProperties properties;
    private final Client client;

    public GeminiTextProvider(GeminiProperties properties) {
        this.properties = properties;
        this.client = createClient(properties);
    }

    @Override
    public String generateText(String prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini AI is disabled or missing API key");
        }

        try {
            GenerateContentResponse response = client.models.generateContent(properties.model(), prompt, null);
            return cleanText(response == null ? null : response.text());
        } catch (ClientException ex) {
            throw new IllegalStateException("Gemini request failed", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return properties.enabled() && StringUtils.hasText(properties.apiKey()) && client != null;
    }

    private Client createClient(GeminiProperties geminiProperties) {
        if (!geminiProperties.enabled()) {
            log.info("Gemini integration is disabled via ai.gemini.enabled=false");
            return null;
        }
        if (!StringUtils.hasText(geminiProperties.apiKey())) {
            log.warn("Gemini API key is not configured. AI guidance will use fallback responses.");
            return null;
        }

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(geminiProperties.timeoutSeconds()))
                .build();

        log.info("Gemini provider initialized with model {}", geminiProperties.model());
        return Client.builder()
                .apiKey(geminiProperties.apiKey())
                .httpOptions(builder -> builder.apiVersion("v1beta"))
                .httpClient(httpClient)
                .build();
    }

    private String cleanText(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return "";
        }
        return rawText.trim();
    }
}
