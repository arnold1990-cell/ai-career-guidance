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
import org.springframework.stereotype.Service;

@Service
public class MultiUniversityPageFetcherService {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EduRiteCrawler/1.0; +https://edurite.ai/bot)";
    private static final String ACCEPT_LANGUAGE = "en-ZA,en;q=0.9";
    private static final int MAX_CHARS_PER_PAGE = 8_000;
    private static final int MAX_LINKS_PER_SEED = 30;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(200);

    private static final Map<String, Integer> LINK_RELEVANCE_TERMS = Map.ofEntries(
            Map.entry("programme", 4),
            Map.entry("program", 4),
            Map.entry("course", 3),
            Map.entry("study", 3),
            Map.entry("faculty", 3),
            Map.entry("admission", 3),
            Map.entry("undergraduate", 2),
            Map.entry("postgraduate", 2),
            Map.entry("qualification", 2),
            Map.entry("degree", 2),
            Map.entry("diploma", 2)
    );

    private final UniversitySourceRegistryService registryService;
    private final UniversityPageClassifier classifier;
    private final UniversityUrlNormalizer urlNormalizer;
    private final UniversityRegistryProperties properties;

    public MultiUniversityPageFetcherService(
            UniversitySourceRegistryService registryService,
            UniversityPageClassifier classifier,
            UniversityUrlNormalizer urlNormalizer,
            UniversityRegistryProperties properties
    ) {
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
            if (discovered.size() >= cappedMaxUrls) {
                break;
            }

            if (crawl().getMaxCrawlDepth() > 0) {
                visited.add(normalizedSeed);
                List<String> rankedLinks = extractRankedInternalLinks(university, normalizedSeed, cappedMaxUrls, visited);
                for (String rankedLink : rankedLinks) {
                    discovered.add(rankedLink);
                    if (discovered.size() >= cappedMaxUrls) {
                        break;
                    }
                }
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
        int maxFetches = Math.min(crawl().getMaxFetchedPagesPerUniversity(), urls.size());

        for (String rawUrl : urls) {
            String url = urlNormalizer.normalize(rawUrl);
            if (url.isBlank() || !seen.add(url)) {
                continue;
            }
            if (results.size() >= maxFetches) {
                break;
            }
            if (!registryService.isAllowedUrl(url)) {
                results.add(new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), false,
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
                document.select("script,style,noscript,header,footer,nav").remove();
                String title = document.title();
                String text = truncate(document.text(), MAX_CHARS_PER_PAGE);
                if (text.isBlank()) {
                    return new UniversitySourcePageResult(url, title, UniversityPageType.UNKNOWN, "", Set.of(), false,
                            "Fetched page is empty after cleanup", UniversityCrawlFailureType.EMPTY_CONTENT);
                }
                UniversityPageType pageType = classifier.classify(title, text);
                Set<String> keywords = classifier.extractKeywords(title, text);
                return new UniversitySourcePageResult(url, title, pageType, text, keywords, true, null, null);
            } catch (IOException ex) {
                lastError = ex;
                if (!isRetryable(ex) || attempt == maxRetries) {
                    break;
                }
                sleep(backoffMillis(attempt));
            }
        }

        UniversityCrawlFailureType failureType = classifyFailure(lastError);
        String reason = lastError == null
                ? "Failed to fetch page"
                : "Failed to fetch page: " + lastError.getClass().getSimpleName();
        return new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), false, reason, failureType);
    }

    private List<String> extractRankedInternalLinks(UniversityRegistryProperties.UniversityRegistryEntry university,
                                                    String seedUrl,
                                                    int maxUrls,
                                                    Set<String> visited) {
        try {
            Document document = connect(seedUrl);
            Map<String, Integer> linkScores = new LinkedHashMap<>();
            document.select("a[href]").forEach(link -> {
                String normalized = urlNormalizer.normalize(link.absUrl("href"));
                if (normalized.isBlank() || visited.contains(normalized)) {
                    return;
                }
                if (!registryService.isAllowedUrlForUniversity(university.getUniversityName(), normalized)) {
                    return;
                }
                if (!isSameHost(seedUrl, normalized)) {
                    return;
                }
                String context = (link.text() + " " + normalized).toLowerCase(Locale.ROOT);
                int score = relevanceScore(context);
                if (score <= 0 || classifier.shouldDeprioritizeLink(normalized, link.text())) {
                    return;
                }
                linkScores.merge(normalized, score, Math::max);
            });

            return linkScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .limit(Math.max(0, Math.min(maxUrls, MAX_LINKS_PER_SEED)))
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private void addCommonPaths(Set<String> discovered,
                                UniversityRegistryProperties.UniversityRegistryEntry university,
                                String seedUrl) {
        URI seedUri;
        try {
            seedUri = URI.create(seedUrl);
        } catch (RuntimeException ex) {
            return;
        }
        if (seedUri.getScheme() == null || seedUri.getHost() == null) {
            return;
        }
        String prefix = seedUri.getScheme() + "://" + seedUri.getHost();
        if (seedUri.getPort() != -1) {
            prefix += ":" + seedUri.getPort();
        }
        for (String candidatePath : crawl().getCandidatePaths()) {
            String path = candidatePath.startsWith("/") ? candidatePath : "/" + candidatePath;
            String candidate = urlNormalizer.normalize(prefix + path);
            if (registryService.isAllowedUrlForUniversity(university.getUniversityName(), candidate)) {
                discovered.add(candidate);
            }
        }
    }

    private boolean isSameHost(String baseUrl, String candidateUrl) {
        try {
            URI base = URI.create(baseUrl);
            URI candidate = URI.create(candidateUrl);
            if (base.getHost() == null || candidate.getHost() == null) {
                return false;
            }
            return base.getHost().equalsIgnoreCase(candidate.getHost());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private int relevanceScore(String context) {
        int score = 0;
        for (Map.Entry<String, Integer> entry : LINK_RELEVANCE_TERMS.entrySet()) {
            if (context.contains(entry.getKey())) {
                score += entry.getValue();
            }
        }
        return score;
    }

    private Document connect(String url) throws IOException {
        Connection connection = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .timeout(crawl().getTimeoutMs())
                .followRedirects(true)
                .ignoreContentType(false);
        return connection.get();
    }

    private boolean isRetryable(IOException ex) {
        if (ex instanceof MalformedURLException || ex instanceof UnsupportedMimeTypeException) {
            return false;
        }
        if (ex instanceof HttpStatusException statusException) {
            int statusCode = statusException.getStatusCode();
            return statusCode >= 500 || statusCode == 429;
        }
        return true;
    }

    private long backoffMillis(int attempt) {
        long factor = 1L << (attempt - 1);
        return INITIAL_BACKOFF.toMillis() * factor;
    }

    private UniversityCrawlFailureType classifyFailure(IOException ex) {
        if (ex == null) {
            return UniversityCrawlFailureType.FETCH_ERROR;
        }
        if (ex instanceof SocketTimeoutException) {
            return UniversityCrawlFailureType.TIMEOUT;
        }
        if (ex instanceof SSLException) {
            return UniversityCrawlFailureType.SSL_ERROR;
        }
        if (ex instanceof HttpStatusException statusException) {
            int statusCode = statusException.getStatusCode();
            if (statusCode == 404) {
                return UniversityCrawlFailureType.NOT_FOUND;
            }
            if (statusCode == 401 || statusCode == 403) {
                return UniversityCrawlFailureType.ACCESS_DENIED;
            }
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
