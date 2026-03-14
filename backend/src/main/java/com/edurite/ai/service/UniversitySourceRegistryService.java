package com.edurite.ai.service;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourceRegistryService {

    private static final int MAX_URLS = 10;

    private static final List<String> DEFAULT_SOURCES = List.of(
            "https://www.unisa.ac.za/sites/corporate/default/Apply-for-admission/Undergraduate-qualifications",
            "https://www.uj.ac.za/studyatuj/undergraduate/",
            "https://www.wits.ac.za/course-finder/undergraduate/",
            "https://www.up.ac.za/programmes"
    );

    private static final Set<String> TRUSTED_HOST_SUFFIXES = Set.of(
            "unisa.ac.za",
            "uj.ac.za",
            "wits.ac.za",
            "up.ac.za",
            "uct.ac.za",
            "sun.ac.za",
            "ru.ac.za",
            "ufs.ac.za",
            "ukzn.ac.za",
            "tut.ac.za"
    );

    public List<String> defaultSources() {
        return DEFAULT_SOURCES;
    }

    public int maxUrls() {
        return MAX_URLS;
    }

    public List<String> sanitizeRequestedUrls(List<String> requestedUrls) {
        List<String> candidateUrls = (requestedUrls == null || requestedUrls.isEmpty()) ? defaultSources() : requestedUrls;
        if (candidateUrls.size() > MAX_URLS) {
            throw new IllegalArgumentException("You can analyse at most " + MAX_URLS + " URLs per request.");
        }

        Set<String> deduped = new LinkedHashSet<>();
        for (String rawUrl : candidateUrls) {
            if (rawUrl == null || rawUrl.isBlank()) {
                continue;
            }
            String trimmed = rawUrl.trim();
            if (!isTrustedUrl(trimmed)) {
                throw new IllegalArgumentException("URL is not part of the trusted allowlist: " + trimmed);
            }
            deduped.add(trimmed);
        }

        if (deduped.isEmpty()) {
            throw new IllegalArgumentException("At least one trusted URL is required.");
        }
        return List.copyOf(deduped);
    }

    public boolean isTrustedUrl(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return TRUSTED_HOST_SUFFIXES.stream().anyMatch(suffix -> normalizedHost.equals(suffix) || normalizedHost.endsWith("." + suffix));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
