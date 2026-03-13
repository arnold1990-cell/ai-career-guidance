package com.edurite.recommendation.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiContentService {

    private final String model;
    private final String apiKey;

    public GeminiContentService(
            @Value("${app.ai.gemini.model:gemini-2.5-flash}") String model,
            @Value("${app.ai.gemini.api-key:}") String apiKey) {
        this.model = model;

        this.apiKey = apiKey;
    }

    public String generateText(String prompt) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("Gemini API key is missing: set app.ai.gemini.api-key");
            }

            Client client = Client.builder().apiKey(apiKey).build();
            GenerateContentResponse response = client.models.generateContent(model, prompt, null);
            String text = response.text();
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Gemini response returned no text content");
            }
            return text;
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Failed to generate Gemini content", exception);
        }
    }
}
