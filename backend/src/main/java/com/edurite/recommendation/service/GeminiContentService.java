package com.edurite.recommendation.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiContentService {

    private final String model;
    private final Client client;

    public GeminiContentService(
            @Value("${app.ai.gemini.model:gemini-2.5-flash}") String model,
            @Value("${app.ai.gemini.api-key:}") String apiKey) {
        this.model = model;
        this.client = apiKey == null || apiKey.isBlank()
                ? Client.builder().build()
                : Client.builder().apiKey(apiKey).build();
    }

    public String generateText(String prompt) {
        try {
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
