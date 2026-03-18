package com.edurite.ai.university;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLException;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
public class MultiUniversityPageFetcherService {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EduRiteCrawler/1.0; +https://edurite.ai/bot)";
    private static final String ACCEPT_LANGUAGE = "en-ZA,en;q=0.9";
    private static final int MAX_CHARS_PER_PAGE = 8_000;
    private static final int MAX_LINKS_PER_SEED = 30;
    private static final int MIN_VISIBLE_TEXT_CHARS = 250;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(200);

    private static final Map<String, Integer> LINK_RELEVANCE_TERMS = Map.ofEntries(
            Map.entry("programme", 4), Map.entry("program", 4), Map.entry("course", 3), Map.entry("study", 3),
            Map.entry("faculty", 3), Map.entry("admission", 3), Map.entry("undergraduate", 2), Map.entry("postgraduate", 2),
            Map.entry("qualification", 2), Map.entry("degree", 2), Map.entry("diploma", 2), Map.entry("requirement", 3)
    );

    private final UniversitySourceRegistryService registryService;
    private final UniversityPageClassifier classifier;
    private final UniversityUrlNormalizer urlNormalizer;
    private final UniversityRegistryProperties properties;

    public MultiUniversityPageFetcherService(UniversitySourceRegistryService registryService, UniversityPageClassifier classifier,
                                             UniversityUrlNormalizer urlNormalizer, UniversityRegistryProperties properties) {
        this.registryService = registryService;
        this.classifier = classifier;
        this.urlNormalizer = urlNormalizer;
        this.properties = properties;
    }

    public List<String> discoverCandidateUrls(UniversityRegistryProperties.UniversityRegistryEntry university, int maxUrls) {
        int cappedMaxUrls = Math.min(Math.max(maxUrls, 1), crawl().getMaxDiscoveredCandidatesPerUniversity());
        Set<String> discovered = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        for (String seedUrl : university.getSeedUrls()) {
            String normalizedSeed = urlNormalizer.normalize(seedUrl);
            if (!registryService.isAllowedUrlForUniversity(university.getUniversityName(), normalizedSeed)) {
                continue;
            }
            discovered.add(normalizedSeed);
            addCommonPaths(discovered, university, normalizedSeed);
            if (crawl().getMaxCrawlDepth() > 0) {
                visited.add(normalizedSeed);
                discovered.addAll(extractRankedInternalLinks(university, normalizedSeed, cappedMaxUrls, visited));
            }
            if (discovered.size() >= cappedMaxUrls) {
                break;
            }
        }
        return discovered.stream().limit(cappedMaxUrls).toList();
    }

    public List<UniversitySourcePageResult> fetchPages(List<String> urls) {
        List<UniversitySourcePageResult> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int maxFetches = Math.min(Math.max(1, crawl().getMaxFetchedPagesPerUniversity()), urls.size());
        for (String rawUrl : urls) {
            String url = urlNormalizer.normalize(rawUrl);
            if (url.isBlank() || !seen.add(url)) {
                continue;
            }
            if (results.size() >= maxFetches) {
                break;
            }
            if (!registryService.isAllowedUrl(url)) {
                results.add(new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false,
                        "URL domain is not allowlisted", UniversityCrawlFailureType.ACCESS_DENIED));
                continue;
            }
            results.add(fetchSingle(url));
        }
        return results;
    }

    private UniversitySourcePageResult fetchSingle(String url) {
        IOException lastError = null;
        int maxRetries = Math.max(1, crawl().getMaxFetchRetries());
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Document document = connect(url);
                cleanup(document);
                String title = document.title();
                String visibleText = extractVisibleText(document);
                List<String> headings = extractHeadings(document);
                if (isJunkPage(url, title, visibleText)) {
                    return new UniversitySourcePageResult(url, title, UniversityPageType.UNKNOWN, "", Set.of(), headings, false,
                            "Page did not contain enough meaningful visible text", UniversityCrawlFailureType.EMPTY_CONTENT);
                }
                UniversityPageType pageType = classifier.classify(title, visibleText);
                Set<String> keywords = classifier.extractKeywords(title, visibleText);
                return new UniversitySourcePageResult(url, title, pageType, visibleText, keywords, headings, true, null, null);
            } catch (IOException ex) {
                lastError = ex;
                if (!isRetryable(ex) || attempt == maxRetries) {
                    break;
                }
                sleep(backoffMillis(attempt));
            }
        }
        return new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false,
                lastError == null ? "Failed to fetch page" : lastError.getMessage(), classifyFailure(lastError));
    }

    private void cleanup(Document document) {
        document.select("script,style,noscript,svg,canvas,header,footer,nav,form,aside,.cookie,.cookies,.banner,.breadcrumb").remove();
    }

    private String extractVisibleText(Document document) {
        Elements candidates = document.select("main, article, .main, .content, .page-content, .entry-content, body");
        String best = "";
        for (Element candidate : candidates) {
            String text = truncate(candidate.text(), MAX_CHARS_PER_PAGE);
            if (text.length() > best.length()) {
                best = text;
            }
        }
        return best;
    }

    private List<String> extractHeadings(Document document) {
        return document.select("h1, h2, h3").stream()
                .map(Element::text)
                .map(text -> text.replaceAll("\\s+", " ").trim())
                .filter(text -> !text.isBlank())
                .distinct()
                .limit(8)
                .toList();
    }

    private boolean isJunkPage(String url, String title, String visibleText) {
        String lower = (url + " " + title + " " + visibleText).toLowerCase(Locale.ROOT);
        if (visibleText.length() < MIN_VISIBLE_TEXT_CHARS) {
            return true;
        }
        return lower.contains("sign in") || lower.contains("log in") || lower.contains("enable javascript")
                || lower.contains("404 not found") || lower.contains("search results") || lower.contains("password");
    }

    private Document connect(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .followRedirects(true)
                .timeout(crawl().getTimeoutMs())
                .get();
    }

    private void addCommonPaths(Set<String> discovered, UniversityRegistryProperties.UniversityRegistryEntry university, String normalizedSeed) {
        URI seedUri = URI.create(normalizedSeed);
        String origin = seedUri.getScheme() + "://" + seedUri.getHost();
        for (String candidatePath : crawl().getCandidatePaths()) {
            String candidate = urlNormalizer.normalize(origin + candidatePath);
            if (registryService.isAllowedUrlForUniversity(university.getUniversityName(), candidate)) {
                discovered.add(candidate);
            }
        }
    }

    private List<String> extractRankedInternalLinks(UniversityRegistryProperties.UniversityRegistryEntry university,
                                                    String seedUrl,
                                                    int limit,
                                                    Set<String> visited) {
        try {
            Document document = connect(seedUrl);
            Map<String, Integer> ranked = new LinkedHashMap<>();
            for (Element anchor : document.select("a[href]")) {
                String absolute = urlNormalizer.normalize(anchor.absUrl("href"));
                if (absolute.isBlank() || visited.contains(absolute) || !registryService.isAllowedUrlForUniversity(university.getUniversityName(), absolute)) {
                    continue;
                }
                ranked.put(absolute, ranked.getOrDefault(absolute, 0) + linkScore(absolute + " " + anchor.text()));
            }
            return ranked.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .limit(Math.min(limit, MAX_LINKS_PER_SEED))
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private int linkScore(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        int score = 0;
        for (Map.Entry<String, Integer> entry : LINK_RELEVANCE_TERMS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }
        return score;
    }

    private boolean isRetryable(IOException ex) {
        if (ex instanceof MalformedURLException || ex instanceof UnsupportedMimeTypeException) return false;
        if (ex instanceof HttpStatusException statusException) {
            int statusCode = statusException.getStatusCode();
            return statusCode == 408 || statusCode == 429 || statusCode >= 500;
        }
        return true;
    }

    private long backoffMillis(int attempt) {
        return INITIAL_BACKOFF.toMillis() * (1L << (attempt - 1));
    }

    private UniversityCrawlFailureType classifyFailure(IOException ex) {
        if (ex == null) return UniversityCrawlFailureType.FETCH_ERROR;
        if (ex instanceof SocketTimeoutException) return UniversityCrawlFailureType.TIMEOUT;
        if (ex instanceof SSLException) return UniversityCrawlFailureType.SSL_ERROR;
        if (ex instanceof HttpStatusException statusException) {
            if (statusException.getStatusCode() == 404) return UniversityCrawlFailureType.NOT_FOUND;
            if (statusException.getStatusCode() == 401 || statusException.getStatusCode() == 403) return UniversityCrawlFailureType.ACCESS_DENIED;
        }
        return UniversityCrawlFailureType.FETCH_ERROR;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private UniversityRegistryProperties.CrawlProperties crawl() {
        return properties.getCrawl();
    }

    private String truncate(String value, int maxChars) {
        if (value == null) return "";
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxChars ? normalized : normalized.substring(0, maxChars);
    }
}
