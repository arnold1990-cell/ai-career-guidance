package com.edurite.ai.service;

final class GeminiModelResolver {

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private GeminiModelResolver() {
    }

    static String resolveModelName(String configuredModel) {
        String normalized = normalizeConfiguredModel(configuredModel);
        return normalized.isEmpty() ? DEFAULT_MODEL : normalized;
    }

    private static String normalizeConfiguredModel(String configuredModel) {
        if (configuredModel == null) {
            return "";
        }

        String normalized = configuredModel.trim();
        if (normalized.isEmpty()) {
            return "";
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

        if (normalized.startsWith("v1/")) {
            normalized = normalized.substring("v1/".length()).trim();
        }

        while (normalized.startsWith("models/")) {
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

        while (normalized.startsWith("models/")) {
            normalized = normalized.substring("models/".length()).trim();
        }

        return normalized.trim();
    }

    static String buildGenerateContentPath(String configuredModel) {
        return "/v1/models/" + resolveModelName(configuredModel) + ":generateContent";
    }
}