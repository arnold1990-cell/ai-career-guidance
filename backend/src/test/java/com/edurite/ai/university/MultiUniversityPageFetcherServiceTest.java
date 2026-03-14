package com.edurite.ai.university;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultiUniversityPageFetcherServiceTest {

    private final MultiUniversityPageFetcherService service = new MultiUniversityPageFetcherService(
            new UniversitySourceRegistryService(),
            new UniversityPageClassifier()
    );

    @Test
    void keepsProcessingWhenOneUrlIsBlocked() {
        var results = service.fetchPages(List.of(
                "https://invalid.example.com/page",
                "https://www.unisa.ac.za/non-existent-page-for-test"
        ));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).success()).isFalse();
    }
}
