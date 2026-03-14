package com.edurite.ai.university;

import java.io.IOException;
import java.util.ArrayList;
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

    public MultiUniversityPageFetcherService(
            UniversitySourceRegistryService registryService,
            UniversityPageClassifier classifier
    ) {
        this.registryService = registryService;
        this.classifier = classifier;
    }

    public List<UniversitySourcePageResult> fetchPages(List<String> urls) {
        List<UniversitySourcePageResult> results = new ArrayList<>();
        for (String url : urls) {
            if (!registryService.isAllowedUrl(url)) {
                results.add(new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), false,
                        "URL domain is not allowlisted"));
                continue;
            }
            try {
                Document document = Jsoup.connect(url)
                        .userAgent("EduRiteBot/1.0 (+academic-guidance)")
                        .timeout(TIMEOUT_MS)
                        .get();
                document.select("script,style,noscript,header,footer,nav").remove();
                String title = document.title();
                String text = truncate(document.text(), MAX_CHARS_PER_PAGE);
                UniversityPageType pageType = classifier.classify(title, text);
                Set<String> keywords = classifier.extractKeywords(title, text);
                results.add(new UniversitySourcePageResult(url, title, pageType, text, keywords, true, null));
            } catch (IOException ex) {
                results.add(new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), false,
                        "Failed to fetch page"));
            }
        }
        return results;
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
