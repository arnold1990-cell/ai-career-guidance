package com.edurite.ai.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiModelNameResolverTest {

    @ParameterizedTest
    @CsvSource({
            "gemini-2.0-flash,gemini-2.0-flash",
            " models/gemini-2.0-flash ,gemini-2.0-flash",
            " /models/gemini-2.0-flash ,gemini-2.0-flash",
            " gemini/gemini-2.0-flash ,gemini-2.0-flash",
            " v1beta/models/gemini-2.0-flash:generateContent ,gemini-2.0-flash"
    })
    void normalizeRemovesUnsupportedPrefixes(String raw, String expected) {
        assertThat(GeminiModelNameResolver.normalize(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "gemini-2.0-flash,/v1beta/models/gemini-2.0-flash:generateContent",
            "models/gemini-2.0-flash,/v1beta/models/gemini-2.0-flash:generateContent"
    })
    void buildGenerateContentPathNeverDuplicatesModelsPrefix(String raw, String expectedPath) {
        assertThat(GeminiModelNameResolver.buildGenerateContentPath(raw)).isEqualTo(expectedPath);
    }
}
