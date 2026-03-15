package com.edurite.ai.university;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultiUniversityPageFetcherServiceTest {

    @Test
    void keepsProcessingAllUrlsWhenSomeAreBlocked() {
        UniversitySourceRegistryService registryService = new UniversitySourceRegistryService(buildProperties(), new UniversityUrlNormalizer());
        MultiUniversityPageFetcherService service = new MultiUniversityPageFetcherService(
                registryService,
                new UniversityPageClassifier(),
                new UniversityUrlNormalizer()
        );

        var results = service.fetchPages(List.of(
                "https://invalid.example.com/page",
                "https://malicious.invalid/phishing",
                "https://www.university-a.ac.za/non-existent-page-for-test"
        ));

        assertThat(results).hasSize(3);
        assertThat(results.get(0).success()).isFalse();
        assertThat(results.get(1).success()).isFalse();
    }

    private UniversityRegistryProperties buildProperties() {
        UniversityRegistryProperties properties = new UniversityRegistryProperties();
        UniversityRegistryProperties.UniversityRegistryEntry entry = new UniversityRegistryProperties.UniversityRegistryEntry();
        entry.setUniversityName("University A");
        entry.setBaseDomain("university-a.ac.za");
        entry.setAllowedDomains(List.of("university-a.ac.za"));
        entry.setSeedUrls(List.of("https://www.university-a.ac.za/programmes"));
        properties.getRegistry().add(entry);
        return properties;
    }
}
