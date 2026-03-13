package com.edurite.ai.service;

final class GeminiModelResolver {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private GeminiModelResolver() {
    }

    static String resolveModelName(String configuredModel) {
        if (configuredModel == null) {
            return DEFAULT_MODEL;
        }

        String normalized = configuredModel.trim();
        if (normalized.isEmpty()) {
            return DEFAULT_MODEL;
        }

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }

        if (normalized.startsWith("models/")) {
            normalized = normalized.substring("models/".length()).trim();
        }

        return normalized.isEmpty() ? DEFAULT_MODEL : normalized;
    }

    static String buildGenerateContentPath(String configuredModel) {
        return "/v1beta/models/" + resolveModelName(configuredModel) + ":generateContent";
    }
}
