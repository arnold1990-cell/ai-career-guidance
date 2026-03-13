package com.edurite.ai.provider;

/**
 * Contract for AI model providers.
 *
 * Keeping this interface small makes it easy to swap Gemini with Vertex AI later.
 */
public interface AiTextProvider {

    String generateText(String prompt);

    boolean isAvailable();
}
