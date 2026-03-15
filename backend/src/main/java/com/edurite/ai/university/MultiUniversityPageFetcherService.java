package com.edurite.ai.university;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Service
public class MultiUniversityPageFetcherService {

    private static final int TIMEOUT_MS = 8_000;
    private static final int MAX_CHARS_PER_PAGE = 8_000;

    private final UniversitySourceRegistryService registryService;
    private final UniversityPageClassifier classifier;
    private final UniversityUrlNormalizer urlNormalizer;

    public MultiUniversityPageFetcherService(
            UniversitySourceRegistryService registryService,
            UniversityPageClassifier classifier,
            UniversityUrlNormalizer urlNormalizer
    ) {
        this.registryService = registryService;
        this.classifier = classifier;
        this.urlNormalizer = urlNormalizer;
    }

    public List<String> discoverCandidateUrls(UniversityRegistryProperties.UniversityRegistryEntry university, int maxUrls) {
        Set<String> discovered = new LinkedHashSet<>();
        for (String seedUrl : university.getSeedUrls()) {
            String normalizedSeed = urlNormalizer.normalize(seedUrl);
            if (!registryService.isAllowedUrlForUniversity(university.getUniversityName(), normalizedSeed)) {
                continue;
            }
            discovered.add(normalizedSeed);
            try {
                Document document = connect(normalizedSeed);
                document.select("a[href]").forEach(link -> {
                    String candidate = urlNormalizer.normalize(link.absUrl("href"));
                    if (candidate.isBlank()) {
                        return;
                    }
                    if (!registryService.isAllowedUrlForUniversity(university.getUniversityName(), candidate)) {
                        return;
                    }
                    if (looksAcademic(candidate)) {
                        discovered.add(candidate);
                    }
                });
            } catch (IOException ignored) {
                // If one seed fails, the orchestrator will continue with the next seed.
            }
            if (discovered.size() >= maxUrls) {
                break;
            }
        }
        return discovered.stream().limit(maxUrls).toList();
    }

    public List<UniversitySourcePageResult> fetchPages(List<String> urls) {
        List<UniversitySourcePageResult> results = new ArrayList<>();
        for (String rawUrl : urls) {
            String url = urlNormalizer.normalize(rawUrl);
            if (!registryService.isAllowedUrl(url)) {
                results.add(new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), false,
                        "URL domain is not allowlisted"));
                continue;
            }
            results.add(fetchSingle(url));
        }
        return results;
    }

    private UniversitySourcePageResult fetchSingle(String url) {
        try {
            Document document = connect(url);
            document.select("script,style,noscript,header,footer,nav").remove();
            String title = document.title();
            String text = truncate(document.text(), MAX_CHARS_PER_PAGE);
            UniversityPageType pageType = classifier.classify(title, text);
            Set<String> keywords = classifier.extractKeywords(title, text);
            return new UniversitySourcePageResult(url, title, pageType, text, keywords, true, null);
        } catch (IOException firstError) {
            try {
                Document retryDocument = connect(url);
                retryDocument.select("script,style,noscript,header,footer,nav").remove();
                String title = retryDocument.title();
                String text = truncate(retryDocument.text(), MAX_CHARS_PER_PAGE);
                UniversityPageType pageType = classifier.classify(title, text);
                Set<String> keywords = classifier.extractKeywords(title, text);
                return new UniversitySourcePageResult(url, title, pageType, text, keywords, true, null);
            } catch (IOException retryError) {
                return new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), false,
                        "Failed to fetch page: " + retryError.getClass().getSimpleName());
            }
        }
    }

    private Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("EduRiteBot/1.0 (+academic-guidance)")
                .timeout(TIMEOUT_MS)
                .get();
    }

    private boolean looksAcademic(String url) {
        String value = url.toLowerCase();
        return value.contains("programme") || value.contains("program") || value.contains("qualification")
                || value.contains("faculty") || value.contains("admission") || value.contains("course")
                || value.contains("undergraduate") || value.contains("postgraduate");
    }

    private String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars);
    }
}
