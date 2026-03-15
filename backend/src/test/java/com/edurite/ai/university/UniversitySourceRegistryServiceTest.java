package com.edurite.ai.university;

import java.util.List;
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
    void exposesMoreThanOneDefaultSource() {
        assertThat(service.getDefaultSources()).hasSizeGreaterThan(1);
    }

    @Test
    void deduplicateKeepsAllUniqueSources() {
        List<String> deduped = service.deduplicate(List.of(
                "https://www.unisa.ac.za/a",
                "https://www.uj.ac.za/b",
                "https://www.unisa.ac.za/a"
        ));

        assertThat(deduped).containsExactly("https://www.unisa.ac.za/a", "https://www.uj.ac.za/b");
    }
}
