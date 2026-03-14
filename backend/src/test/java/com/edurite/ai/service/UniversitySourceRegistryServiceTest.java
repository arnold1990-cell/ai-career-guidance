package com.edurite.ai.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UniversitySourceRegistryServiceTest {

    private final UniversitySourceRegistryService service = new UniversitySourceRegistryService();

    @Test
    void defaultsReturnedWhenUrlsMissing() {
        assertThat(service.sanitizeRequestedUrls(null)).isNotEmpty();
    }

    @Test
    void blocksUntrustedDomain() {
        assertThatThrownBy(() -> service.sanitizeRequestedUrls(List.of("https://evil.example.com/page")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trusted allowlist");
    }

    @Test
    void enforcesMaxUrlLimit() {
        assertThatThrownBy(() -> service.sanitizeRequestedUrls(List.of(
                "https://www.unisa.ac.za/a1", "https://www.unisa.ac.za/a2", "https://www.unisa.ac.za/a3",
                "https://www.unisa.ac.za/a4", "https://www.unisa.ac.za/a5", "https://www.unisa.ac.za/a6",
                "https://www.unisa.ac.za/a7", "https://www.unisa.ac.za/a8", "https://www.unisa.ac.za/a9",
                "https://www.unisa.ac.za/a10", "https://www.unisa.ac.za/a11"
        ))).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("at most");
    }
}
