package com.edurite.ai.service;

final class GeminiModelNameResolver {

    private GeminiModelNameResolver() {
    }

    static String normalize(String rawModel) {
        if (rawModel == null) {
            return "";
        }

        String normalized = rawModel.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        normalized = normalized.replace('\\', '/');
        normalized = normalized.replaceFirst("^/+(?=.)", "");
        normalized = normalized.replaceFirst("^v1beta/", "");
        normalized = normalized.replaceFirst("^models/", "");
        normalized = normalized.replaceFirst("^gemini/", "");
        normalized = normalized.replaceFirst(":generateContent$", "");

        return normalized;
    }

    static String buildGenerateContentPath(String rawModel) {
        return "/v1beta/models/" + normalize(rawModel) + ":generateContent";
    }
}
