package com.edurite.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiModelResolverTest {

    @Test
    void resolveModelNameNormalizesCommonAccidentalPrefixes() {
        assertThat(GeminiModelResolver.resolveModelName("gemini-2.0-flash"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("  /models/gemini-2.0-flash  "))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("/gemini-2.0-flash"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("models/gemini-2.0-flash"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("models/models/gemini-2.0-flash"))
                .isEqualTo("gemini-2.0-flash");
    }

    @Test
    void resolveModelNameNormalizesEndpointStyleValuesFromConfiguration() {
        assertThat(GeminiModelResolver.resolveModelName("models/gemini-2.0-flash:generateContent"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("v1beta/models/gemini-2.0-flash:generateContent"))
                .isEqualTo("gemini-2.0-flash");
        assertThat(GeminiModelResolver.resolveModelName("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=test"))
                .isEqualTo("gemini-2.0-flash");
    }

    @Test
    void resolveModelNameFallsBackToDefaultForNullOrBlank() {
        assertThat(GeminiModelResolver.resolveModelName(null)).isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("")).isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("   ")).isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("models/   ")).isEqualTo("gemini-2.5-flash");
        assertThat(GeminiModelResolver.resolveModelName("/models/   ")).isEqualTo("gemini-2.5-flash");
    }

    @Test
    void buildGenerateContentPathAlwaysBuildsSingleModelsSegment() {
        assertThat(GeminiModelResolver.buildGenerateContentPath("gemini-2.0-flash"))
                .isEqualTo("/v1beta/models/gemini-2.0-flash:generateContent");
        assertThat(GeminiModelResolver.buildGenerateContentPath("models/gemini-2.0-flash"))
                .isEqualTo("/v1beta/models/gemini-2.0-flash:generateContent");
        assertThat(GeminiModelResolver.buildGenerateContentPath("/models/gemini-2.0-flash"))
                .isEqualTo("/v1beta/models/gemini-2.0-flash:generateContent");
        assertThat(GeminiModelResolver.buildGenerateContentPath("v1beta/models/gemini-2.0-flash:generateContent"))
                .isEqualTo("/v1beta/models/gemini-2.0-flash:generateContent");
        assertThat(GeminiModelResolver.buildGenerateContentPath("models/models/gemini-2.0-flash"))
                .isEqualTo("/v1beta/models/gemini-2.0-flash:generateContent");
    }
    @Test
    void buildGenerateContentPathContainsExactlyOneModelsSegment() {
        String path = GeminiModelResolver.buildGenerateContentPath("models/models/gemini-2.0-flash");

        assertThat(path).contains("/v1beta/models/");
        assertThat(path).doesNotContain("/models/models/");
    }

}
