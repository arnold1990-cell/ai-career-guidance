package com.edurite.ai.university;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourceRegistryService {

    private static final List<String> DEFAULT_SOURCES = List.of(
            "https://www.unisa.ac.za/sites/corporate/default/Register-to-study-through-Unisa/Undergraduate-qualifications",
            "https://www.uj.ac.za/studyatuj/undergraduate/"
    );

    private static final Set<String> TRUSTED_DOMAINS = Set.of(
            "unisa.ac.za", "uj.ac.za", "wits.ac.za", "uct.ac.za", "up.ac.za", "sun.ac.za"
    );

    public List<String> getDefaultSources() {
        return DEFAULT_SOURCES;
    }

    public boolean isAllowedUrl(String url) {
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return false;
            }
            if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return false;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return TRUSTED_DOMAINS.stream().anyMatch(domain -> normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public List<String> deduplicate(List<String> urls) {
        return new LinkedHashSet<>(urls).stream().toList();
    }
}
