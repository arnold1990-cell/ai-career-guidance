package com.edurite.ai.university;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversitySourceRegistryServiceTest {

    private final UniversitySourceRegistryService service = new UniversitySourceRegistryService();

    @Test
    void allowsTrustedDomainAndBlocksUnknownDomain() {
        assertThat(service.isAllowedUrl("https://www.unisa.ac.za/programmes")).isTrue();
        assertThat(service.isAllowedUrl("https://evil.example.com/unisa.ac.za")).isFalse();
    }

    @Test
    void exposesDefaultSources() {
        assertThat(service.getDefaultSources()).isNotEmpty();
    }
}
