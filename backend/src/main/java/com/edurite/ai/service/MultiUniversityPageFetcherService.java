package com.edurite.ai.service;

import com.edurite.ai.dto.FetchedUniversityPage;
import com.edurite.ai.dto.UniversityPageType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class MultiUniversityPageFetcherService {

    private static final int REQUEST_TIMEOUT_MS = 8000;
    private static final int MAX_TEXT_PER_PAGE = 4500;
    private static final String USER_AGENT = "EduRiteAcademicBot/1.0 (+https://edurite.local)";

    private final UniversitySourceRegistryService sourceRegistryService;
    private final UniversityPageClassifier pageClassifier;

    public MultiUniversityPageFetcherService(UniversitySourceRegistryService sourceRegistryService,
                                             UniversityPageClassifier pageClassifier) {
        this.sourceRegistryService = sourceRegistryService;
        this.pageClassifier = pageClassifier;
    }

    public List<FetchedUniversityPage> fetchPages(List<String> urls) {
        List<FetchedUniversityPage> pages = new ArrayList<>();
        for (String url : urls) {
            if (!sourceRegistryService.isTrustedUrl(url)) {
                pages.add(new FetchedUniversityPage(url, null, UniversityPageType.UNKNOWN, "", List.of(), List.of(), false,
                        "URL is not trusted."));
                continue;
            }
            try {
                Document document = Jsoup.connect(url)
                        .timeout(REQUEST_TIMEOUT_MS)
                        .userAgent(USER_AGENT)
                        .get();
                document.select("script, style, nav, footer, header, noscript").remove();
                String title = cleanText(document.title(), 200);
                String bodyText = cleanText(Jsoup.clean(document.body().text(), Safelist.none()), MAX_TEXT_PER_PAGE);
                UniversityPageType pageType = pageClassifier.classify(title, bodyText);

                pages.add(new FetchedUniversityPage(
                        url,
                        title,
                        pageType,
                        bodyText,
                        pageClassifier.extractKeywords(title, bodyText),
                        pageClassifier.extractNotes(bodyText),
                        true,
                        null
                ));
            } catch (IOException ex) {
                pages.add(new FetchedUniversityPage(url, null, UniversityPageType.UNKNOWN, "", List.of(), List.of(), false,
                        "Failed to fetch page."));
            }
        }
        return pages;
    }

    private String cleanText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
