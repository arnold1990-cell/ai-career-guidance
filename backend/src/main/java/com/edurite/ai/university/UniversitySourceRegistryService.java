package com.edurite.ai.university;

import java.net.URI;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    public List<UniversitySourceDefinition> getActiveDefinitions() {
        return getActiveUniversities().stream().map(this::toDefinition).toList();
    }

    public UniversitySourceDefinition toDefinition(UniversityRegistryProperties.UniversityRegistryEntry entry) {
        return new UniversitySourceDefinition(
                slug(entry.getUniversityName()),
                entry.getUniversityName(),
                entry.getBaseDomain(),
                safe(entry.getAllowedDomains()),
                safe(entry.getOfficialHomepages()),
                safe(entry.getProgrammePages()),
                safe(entry.getAdmissionsPages()),
                safe(entry.getFacultyPages()),
                safe(entry.getDiscoveryPages()),
                safe(entry.getSitemapUrls()),
                safe(entry.getBlockedPatterns()),
                safe(entry.getAllowedPatterns()),
                safe(entry.getQualificationLevelsSupported()),
                parsePriority(entry.getSourcePriority()),
                entry.getCrawlPriority(),
                entry.getMaxPagesToFetch(),
                entry.isActive()
        );
    }

    public List<String> getDefaultSources() {
        return getActiveDefinitions().stream()
                .flatMap(definition -> definition.seedUrls().stream())
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .distinct()
                .limit(100)
                .toList();
    }

    public boolean isAllowedUrl(String url) {
        String host = host(url);
        if (host == null) {
            return false;
        }
        return getActiveDefinitions().stream().anyMatch(definition -> matchesAllowedDomain(host, definition));
    }

    public boolean isAllowedUrlForUniversity(String universityName, String url) {
        UniversitySourceDefinition definition = getActiveDefinitions().stream()
                .filter(item -> item.displayName().equalsIgnoreCase(universityName))
                .findFirst()
                .orElse(null);
        return definition != null && isAllowedUrlForDefinition(definition, url);
    }

    public boolean isAllowedUrlForDefinition(UniversitySourceDefinition definition, String url) {
        String normalized = urlNormalizer.normalize(url);
        String host = host(normalized);
        if (host == null || !matchesAllowedDomain(host, definition)) {
            return false;
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (definition.blockedPatterns().stream().map(String::toLowerCase).anyMatch(lowered::contains)) {
            return false;
        }
        if (!definition.allowedPatterns().isEmpty() && definition.allowedPatterns().stream().map(String::toLowerCase).noneMatch(lowered::contains)) {
            return false;
        }
        return true;
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

    private boolean matchesAllowedDomain(String normalizedHost, UniversitySourceDefinition definition) {
        Set<String> domains = new LinkedHashSet<>();
        domains.add(definition.baseDomain().toLowerCase(Locale.ROOT));
        domains.addAll(definition.allowedDomains().stream().map(value -> value.toLowerCase(Locale.ROOT)).toList());
        return domains.stream().anyMatch(domain -> normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain));
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    private SourcePriority parsePriority(String value) {
        try {
            return value == null ? SourcePriority.STANDARD : SourcePriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SourcePriority.STANDARD;
        }
    }

    private String slug(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
