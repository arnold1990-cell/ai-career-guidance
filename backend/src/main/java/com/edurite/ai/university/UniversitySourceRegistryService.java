package com.edurite.ai.university;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourceRegistryService {

    private final UniversityRegistryProperties properties;
    private final UniversityUrlNormalizer urlNormalizer;

    public UniversitySourceRegistryService(UniversityRegistryProperties properties, UniversityUrlNormalizer urlNormalizer) {
        this.properties = properties;
        this.urlNormalizer = urlNormalizer;
    }

    public List<UniversityRegistryProperties.UniversityRegistryEntry> getActiveUniversities() {
        return properties.getRegistry().stream()
                .filter(UniversityRegistryProperties.UniversityRegistryEntry::isActive)
                .sorted(Comparator.comparingInt(UniversityRegistryProperties.UniversityRegistryEntry::getCrawlPriority)
                        .thenComparing(UniversityRegistryProperties.UniversityRegistryEntry::getUniversityName))
                .toList();
    }

    public List<String> getDefaultSources() {
        return getActiveUniversities().stream()
                .flatMap(entry -> entry.getAllEntryPoints().stream())
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .distinct()
                .limit(150)
                .toList();
    }

    public Optional<UniversityRegistryProperties.UniversityRegistryEntry> findByUrl(String url) {
        String host = host(url);
        if (host == null) {
            return Optional.empty();
        }
        return getActiveUniversities().stream().filter(university -> matchesAllowedDomain(host, university)).findFirst();
    }

    public boolean isAllowedUrl(String url) {
        return findByUrl(url).isPresent();
    }

    public boolean isAllowedUrlForUniversity(String universityName, String url) {
        String host = host(url);
        if (host == null) {
            return false;
        }
        return getActiveUniversities().stream()
                .filter(university -> university.getUniversityName().equalsIgnoreCase(universityName)
                        || university.getInstitutionKey().equalsIgnoreCase(universityName))
                .anyMatch(university -> matchesAllowedDomain(host, university));
    }

    public List<String> deduplicate(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        return urls.stream()
                .filter(Objects::nonNull)
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    }

    public int configuredUniversityCount() {
        return properties.getRegistry().size();
    }

    public int configuredInstitutionCount() {
        return getActiveUniversities().size();
    }

    public String inferInstitutionName(String url) {
        return findByUrl(url).map(UniversityRegistryProperties.UniversityRegistryEntry::getUniversityName).orElse("Institution Source");
    }

    public List<String> officialEntryPoints(UniversityRegistryProperties.UniversityRegistryEntry entry) {
        List<String> entryPoints = new ArrayList<>();
        entryPoints.addAll(entry.getAllEntryPoints());
        return deduplicate(entryPoints);
    }

    private String host(String url) {
        try {
            URI uri = URI.create(urlNormalizer.normalize(url));
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return null;
            }
            return host.toLowerCase(Locale.ROOT);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean matchesAllowedDomain(String normalizedHost, UniversityRegistryProperties.UniversityRegistryEntry university) {
        Set<String> domains = new LinkedHashSet<>();
        domains.add(university.getBaseDomain().toLowerCase(Locale.ROOT));
        domains.addAll(university.getAllowedDomains().stream().map(value -> value.toLowerCase(Locale.ROOT)).toList());
        return domains.stream().anyMatch(domain -> normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain));
    }
}
