package com.edurite.ai.service;

final class GeminiModelResolver {

    private static final String DEFAULT_MODEL = "gemini-1.5-flash";

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

        if (normalized.startsWith("https://") || normalized.startsWith("http://")) {
            int modelsIndex = normalized.indexOf("/models/");
            if (modelsIndex >= 0) {
                normalized = normalized.substring(modelsIndex + 1).trim();
            }
        }

        if (normalized.startsWith("v1beta/")) {
            normalized = normalized.substring("v1beta/".length()).trim();
        }

        if (normalized.startsWith("models/")) {
            normalized = normalized.substring("models/".length()).trim();
        }

        int queryStart = normalized.indexOf('?');
        if (queryStart >= 0) {
            normalized = normalized.substring(0, queryStart).trim();
        }

        int actionStart = normalized.indexOf(':');
        if (actionStart >= 0) {
            normalized = normalized.substring(0, actionStart).trim();
        }

        return normalized.isEmpty() ? DEFAULT_MODEL : normalized;
    }

    static String buildGenerateContentPath(String configuredModel) {
        return "/v1beta/models/" + resolveModelName(configuredModel) + ":generateContent";
    }
}
