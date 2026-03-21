package com.edurite.ai.university;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversitySourceRegistryServiceTest {

    @Test
    void mapsEntriesIntoValidatedSourceDefinitions() {
        UniversitySourceRegistryService service = new UniversitySourceRegistryService(buildProperties(2), new UniversityUrlNormalizer());

        UniversitySourceDefinition definition = service.getActiveDefinitions().get(0);

        assertThat(definition.displayName()).isEqualTo("University 1");
        assertThat(definition.programmePages()).contains("https://www.university-1.ac.za/programmes");
        assertThat(definition.sourcePriority()).isEqualTo(SourcePriority.HIGH);
    }

    @Test
    void validatesAllowedDomainsAndBlockedPatterns() {
        UniversitySourceRegistryService service = new UniversitySourceRegistryService(buildProperties(2), new UniversityUrlNormalizer());
        UniversitySourceDefinition definition = service.getActiveDefinitions().get(0);

        assertThat(service.isAllowedUrl("https://www.university-1.ac.za/programmes")).isTrue();
        assertThat(service.isAllowedUrlForDefinition(definition, "https://www.university-1.ac.za/student-portal")).isFalse();
        assertThat(service.isAllowedUrl("https://evil.example.com/programmes")).isFalse();
    }

    @Test
    void deduplicateNormalizesEquivalentUrls() {
        UniversitySourceRegistryService service = new UniversitySourceRegistryService(buildProperties(2), new UniversityUrlNormalizer());

        List<String> deduped = service.deduplicate(List.of(
                "https://www.university-1.ac.za/programmes/",
                "https://www.university-1.ac.za/programmes?utm_source=ads",
                "https://www.university-2.ac.za/a"
        ));

        assertThat(deduped).containsExactly(
                "https://www.university-1.ac.za/programmes",
                "https://www.university-2.ac.za/a"
        );
    }

    private UniversityRegistryProperties buildProperties(int count) {
        UniversityRegistryProperties properties = new UniversityRegistryProperties();
        for (int index = 1; index <= count; index++) {
            UniversityRegistryProperties.UniversityRegistryEntry entry = new UniversityRegistryProperties.UniversityRegistryEntry();
            entry.setUniversityName("University " + index);
            entry.setBaseDomain("university-" + index + ".ac.za");
            entry.setAllowedDomains(List.of("university-" + index + ".ac.za"));
            entry.setOfficialHomepages(List.of("https://www.university-" + index + ".ac.za/"));
            entry.setProgrammePages(List.of("https://www.university-" + index + ".ac.za/programmes"));
            entry.setAdmissionsPages(List.of("https://www.university-" + index + ".ac.za/admissions"));
            entry.setBlockedPatterns(List.of("portal", "login"));
            entry.setQualificationLevelsSupported(List.of("Undergraduate"));
            entry.setSourcePriority("HIGH");
            entry.setActive(true);
            entry.setCrawlPriority(index);
            properties.getRegistry().add(entry);
        }
        return properties;
    }
}
