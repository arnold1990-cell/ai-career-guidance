package com.edurite.ai.university;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourceRegistryService {

    private static final Logger log = LoggerFactory.getLogger(UniversitySourceRegistryService.class);

    private final UniversityRegistryProperties properties;
    private final UniversityUrlNormalizer urlNormalizer;

    public UniversitySourceRegistryService(UniversityRegistryProperties properties, UniversityUrlNormalizer urlNormalizer) {
        this.properties = properties;
        this.urlNormalizer = urlNormalizer;
    }

    @PostConstruct
    void logRegistryStatus() {
        int configured = configuredUniversityCount();
        List<UniversityRegistryProperties.UniversityRegistryEntry> activeUniversities = getActiveUniversities();
        long seedUrls = activeUniversities.stream()
                .map(UniversityRegistryProperties.UniversityRegistryEntry::getSeedUrls)
                .filter(Objects::nonNull)
                .mapToLong(List::size)
                .sum();
        long curatedUrls = activeUniversities.stream()
                .mapToLong(university -> curatedSourcesFor(university, 32).size())
                .sum();
        log.info("University registry initialised: configuredUniversities={}, activeUniversities={}, seedUrls={}, curatedOfficialUrls={}",
                configured, activeUniversities.size(), seedUrls, curatedUrls);
        if (activeUniversities.isEmpty()) {
            log.error("University registry is empty or all entries are inactive. University AI guidance cannot discover official sources until configuration is fixed.");
        } else if (curatedUrls == 0) {
            log.error("University registry loaded institutions but none expose curated official URLs. Requested sources would remain zero until registry seed URLs or candidate paths are fixed.");
        }
    }

    public List<UniversityRegistryProperties.UniversityRegistryEntry> getActiveUniversities() {
        return properties.getRegistry().stream()
                .filter(UniversityRegistryProperties.UniversityRegistryEntry::isActive)
                .sorted(Comparator.comparingInt(UniversityRegistryProperties.UniversityRegistryEntry::getCrawlPriority)
                        .thenComparing(UniversityRegistryProperties.UniversityRegistryEntry::getUniversityName))
                .toList();
    }

    public List<String> getDefaultSources() {
        return getFallbackSources(100);
    }

    public List<String> getFallbackSources(int maxUrls) {
        int limit = Math.max(1, maxUrls);
        List<String> fallbackSources = getActiveUniversities().stream()
                .flatMap(entry -> curatedSourcesFor(entry, limit).stream())
                .distinct()
                .limit(limit)
                .toList();
        if (fallbackSources.isEmpty()) {
            log.warn("University registry fallback sources are empty: limit={}, configuredUniversities={}, activeUniversities={}",
                    limit, configuredUniversityCount(), getActiveUniversities().size());
        }
        return fallbackSources;
    }

    public List<String> curatedSourcesFor(UniversityRegistryProperties.UniversityRegistryEntry university, int maxUrls) {
        if (university == null) {
            return List.of();
        }
        int limit = Math.max(1, maxUrls);
        LinkedHashSet<String> curated = new LinkedHashSet<>();
        List<String> seedUrls = university.getSeedUrls() == null ? List.of() : university.getSeedUrls();
        for (String seedUrl : seedUrls) {
            String normalizedSeed = urlNormalizer.normalize(seedUrl);
            if (normalizedSeed.isBlank() || !isAllowedUrlForUniversity(university.getUniversityName(), normalizedSeed)) {
                continue;
            }
            curated.add(normalizedSeed);
            curated.addAll(buildOfficialEntryPoints(university, normalizedSeed));
            if (curated.size() >= limit) {
                break;
            }
        }
        if (curated.isEmpty()) {
            log.warn("University registry entry has no curated official sources after normalization: university={}, seedUrls={}",
                    university.getUniversityName(), seedUrls.size());
        }
        return curated.stream().limit(limit).toList();
    }

    public boolean isAllowedUrl(String url) {
        String host = host(url);
        if (host == null) {
            return false;
        }
        return properties.getRegistry().stream().anyMatch(university -> matchesAllowedDomain(host, university));
    }

    public boolean isAllowedUrlForUniversity(String universityName, String url) {
        String host = host(url);
        if (host == null) {
            return false;
        }
        return properties.getRegistry().stream()
                .filter(university -> university.getUniversityName().equalsIgnoreCase(universityName))
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

    private List<String> buildOfficialEntryPoints(UniversityRegistryProperties.UniversityRegistryEntry university, String normalizedSeedUrl) {
        LinkedHashSet<String> officialUrls = new LinkedHashSet<>();
        String basePrefix = basePrefix(normalizedSeedUrl);
        if (basePrefix.isBlank()) {
            return List.of();
        }
        for (String candidatePath : candidatePaths()) {
            String path = candidatePath == null ? "" : candidatePath.trim();
            if (path.isBlank()) {
                continue;
            }
            String normalized = urlNormalizer.normalize(basePrefix + (path.startsWith("/") ? path : "/" + path));
            if (!normalized.isBlank() && isAllowedUrlForUniversity(university.getUniversityName(), normalized)) {
                officialUrls.add(normalized);
            }
        }
        return new ArrayList<>(officialUrls);
    }

    private List<String> candidatePaths() {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        paths.add("/");
        paths.add("/admissions");
        paths.add("/apply");
        paths.add("/programmes");
        paths.add("/programs");
        paths.add("/study");
        paths.add("/courses");
        paths.add("/faculties");
        paths.add("/qualifications");
        paths.addAll(properties.getCrawl().getCandidatePaths());
        return new ArrayList<>(paths);
    }

    private String basePrefix(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            String prefix = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1) {
                prefix += ":" + uri.getPort();
            }
            return prefix;
        } catch (RuntimeException ex) {
            return "";
        }
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
