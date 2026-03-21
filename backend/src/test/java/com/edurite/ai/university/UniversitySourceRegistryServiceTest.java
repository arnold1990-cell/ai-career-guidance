package com.edurite.ai.university;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class UniversitySourceRegistryServiceTest {

    @Test
    void bindsSouthAfricanRegistryFromApplicationYaml() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        loader.load("application", new ClassPathResource("application.yml"))
                .forEach(propertySource -> environment.getPropertySources().addLast(propertySource));

        UniversityRegistryProperties properties = Binder.get(environment)
                .bind("edurite.university", UniversityRegistryProperties.class)
                .orElseThrow();
        UniversitySourceRegistryService service = new UniversitySourceRegistryService(properties, new UniversityUrlNormalizer());

        assertThat(service.configuredUniversityCount()).isGreaterThan(20);
        assertThat(service.getActiveUniversities()).isNotEmpty();
        assertThat(service.getFallbackSources(12)).isNotEmpty();
    }

    @Test
    void supportsLargeRegistryAndValidatesDomains() {
        UniversitySourceRegistryService service = new UniversitySourceRegistryService(buildProperties(50), new UniversityUrlNormalizer());

        assertThat(service.configuredUniversityCount()).isEqualTo(50);
        assertThat(service.getDefaultSources()).hasSizeGreaterThan(40);
        assertThat(service.isAllowedUrl("https://www.university-1.ac.za/programmes")).isTrue();
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
            entry.setSeedUrls(List.of("https://www.university-" + index + ".ac.za/programmes"));
            entry.setQualificationLevelsSupported(List.of("Undergraduate"));
            entry.setActive(true);
            entry.setCrawlPriority(index);
            properties.getRegistry().add(entry);
        }
        return properties;
    }
}
