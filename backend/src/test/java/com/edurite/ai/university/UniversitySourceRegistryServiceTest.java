package com.edurite.ai.university;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UniversitySourceRegistryServiceTest {

    @Test
    void findsOfficialInstitutionByAllowlistedUrl() {
        UniversityRegistryProperties properties = new UniversityRegistryProperties();
        UniversityRegistryProperties.UniversityRegistryEntry entry = new UniversityRegistryProperties.UniversityRegistryEntry();
        entry.setUniversityName("University of Johannesburg");
        entry.setBaseDomain("uj.ac.za");
        entry.setAllowedDomains(List.of("uj.ac.za"));
        entry.setOfficialHomepages(List.of("https://www.uj.ac.za/"));
        properties.setRegistry(List.of(entry));
        UniversitySourceRegistryService service = new UniversitySourceRegistryService(properties, new UniversityUrlNormalizer());

        assertThat(service.isAllowedUrl("https://www.uj.ac.za/faculties/engineering")).isTrue();
        assertThat(service.inferInstitutionName("https://www.uj.ac.za/faculties/engineering")).isEqualTo("University of Johannesburg");
    }
}
