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
import org.springframework.stereotype.Service;

@Service
public class MultiUniversityPageFetcherService {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EduRiteCrawler/2.0; +https://edurite.ai/bot)";
    private static final String ACCEPT_LANGUAGE = "en-ZA,en;q=0.9";
    private static final String ACCEPT = "text/html,application/xhtml+xml";
    private static final int MAX_CHARS_PER_PAGE = 8_000;
    private static final int MAX_HEADINGS = 8;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(250);

    private static final Map<String, Integer> LINK_RELEVANCE_TERMS = Map.ofEntries(
            Map.entry("programme", 5), Map.entry("program", 5), Map.entry("admission", 4), Map.entry("study", 4),
            Map.entry("degree", 3), Map.entry("faculty", 3), Map.entry("undergraduate", 3), Map.entry("postgraduate", 2),
            Map.entry("qualification", 3), Map.entry("requirements", 3), Map.entry("apply", 2), Map.entry("prospectus", 2)
    );
    private static final List<String> BLOCKED_LINK_HINTS = List.of(
            "login", "log in", "sign in", "portal", "account", "staff", "student-portal", "search?", "mailto:",
            ".pdf", ".jpg", ".png", ".zip", "facebook.com", "linkedin.com", "instagram.com", "youtube.com"
    );
    private static final List<String> PROTECTED_PAGE_HINTS = List.of(
            "login", "log in", "sign in", "password", "student portal", "staff portal", "access denied", "forbidden", "captcha", "cloudflare"
    );
    private static final List<String> ACADEMIC_CONTENT_HINTS = List.of(
            "programme", "program", "admission", "degree", "diploma", "entry requirements", "faculty", "undergraduate", "postgraduate", "aps"
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
        return discoverCandidateUrls(registryService.toDefinition(university), maxUrls);
    }

    public List<String> discoverCandidateUrls(UniversitySourceDefinition definition, int maxUrls) {
        int cap = Math.min(Math.max(maxUrls, 1), Math.min(crawl().getMaxDiscoveredCandidatesPerUniversity(), definition.maxPagesToFetch()));
        Set<String> discovered = new LinkedHashSet<>();
        definition.seedUrls().stream().map(urlNormalizer::normalize)
                .filter(url -> registryService.isAllowedUrlForDefinition(definition, url))
                .forEach(discovered::add);

        List<String> discoverySeeds = new ArrayList<>();
        discoverySeeds.addAll(definition.discoveryPages());
        if (discoverySeeds.isEmpty()) {
            discoverySeeds.addAll(definition.officialHomepages());
        }

        for (String seed : discoverySeeds) {
            if (discovered.size() >= cap) {
                break;
            }
            discovered.addAll(extractRankedInternalLinks(definition, seed, cap - discovered.size()));
        }
        return discovered.stream().limit(cap).toList();
    }

    public List<UniversitySourcePageResult> fetchPages(List<String> urls) {
        List<UniversitySourcePageResult> results = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int maxFetches = Math.max(1, Math.min(crawl().getMaxFetchedPagesPerUniversity(), urls.size()));
        int successes = 0;
        int failures = 0;

        for (String rawUrl : urls) {
            String url = urlNormalizer.normalize(rawUrl);
            if (url.isBlank() || !seen.add(url)) {
                continue;
            }
            if (results.size() >= maxFetches || successes >= crawl().getMaxSuccessfulPagesPerUniversity() || failures >= crawl().getMaxFailedPagesPerUniversity()) {
                break;
            }
            UniversitySourcePageResult result = registryService.isAllowedUrl(url)
                    ? fetchSingle(url)
                    : new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false,
                    "Source is outside the verified university registry.", UniversityCrawlFailureType.ACCESS_DENIED);
            results.add(result);
            if (result.success()) {
                successes++;
            } else {
                failures++;
            }
            sleep(crawl().getPoliteDelayMs());
        }
        return results;
    }

    private UniversitySourcePageResult fetchSingle(String url) {
        IOException lastError = null;
        int maxRetries = Math.max(1, crawl().getMaxFetchRetries());
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Document document = connect(url);
                String title = truncate(document.title(), 200);
                String visibleText = extractVisibleBodyText(document);
                List<String> headings = extractHeadings(document);
                if (looksProtected(document)) {
                    return new UniversitySourcePageResult(url, title, UniversityPageType.UNKNOWN, visibleText, Set.of(), headings, false,
                            "Source requires authentication or blocks automated access.", UniversityCrawlFailureType.PROTECTED);
                }
                if (visibleText.isBlank()) {
                    return new UniversitySourcePageResult(url, title, UniversityPageType.UNKNOWN, "", Set.of(), headings, false,
                            "Source returned no meaningful academic content.", UniversityCrawlFailureType.NO_RESULT);
                }
                UniversityPageType pageType = classifier.classify(url, title, visibleText);
                Set<String> keywords = classifier.extractKeywords(title, visibleText);
                if (isThinContent(title, visibleText, pageType, keywords)) {
                    return new UniversitySourcePageResult(url, title, pageType, visibleText, keywords, headings, false,
                            "Source was too thin to verify programme or admissions details.", UniversityCrawlFailureType.THIN_CONTENT);
                }
                if (classifier.shouldSkipPage(url, title, visibleText)) {
                    return new UniversitySourcePageResult(url, title, pageType, visibleText, keywords, headings, false,
                            "Source did not contain usable academic guidance.", UniversityCrawlFailureType.IRRELEVANT_CONTENT);
                }
                return new UniversitySourcePageResult(url, title, pageType, visibleText, keywords, headings, true, null, null);
            } catch (IOException ex) {
                lastError = ex;
                if (!isRetryable(ex) || attempt >= maxRetries) {
                    break;
                }
                sleep(backoffMillis(attempt));
            }
        }
        return new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false,
                friendlyFailure(classifyFailure(lastError)), classifyFailure(lastError));
    }

    private List<String> extractRankedInternalLinks(UniversitySourceDefinition definition, String seedUrl, int maxUrls) {
        try {
            Document document = connect(seedUrl);
            Map<String, Integer> linkScores = new LinkedHashMap<>();
            document.select("a[href]").forEach(link -> {
                String normalized = normalizeAgainst(seedUrl, link.attr("href"));
                if (normalized.isBlank() || !registryService.isAllowedUrlForDefinition(definition, normalized) || !isSameHost(seedUrl, normalized)) {
                    return;
                }
                String context = (link.text() + " " + normalized).toLowerCase(Locale.ROOT);
                if (BLOCKED_LINK_HINTS.stream().anyMatch(context::contains)) {
                    return;
                }
                if (classifier.shouldDeprioritizeLink(normalized, link.text())) {
                    return;
                }
                int score = relevanceScore(context);
                if (score > 0) {
                    linkScores.merge(normalized, score, Math::max);
                }
            });
            return linkScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .limit(Math.min(maxUrls, crawl().getMaxCandidateLinksPerPage()))
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private String normalizeAgainst(String seedUrl, String href) {
        try {
            URI base = URI.create(seedUrl);
            return urlNormalizer.normalize(base.resolve(href).toString());
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private boolean isThinContent(String title, String visibleText, UniversityPageType pageType, Set<String> keywords) {
        if (pageType != UniversityPageType.UNKNOWN || !keywords.isEmpty()) {
            return false;
        }
        String combined = (title + " " + visibleText).toLowerCase(Locale.ROOT);
        return visibleText.length() < 250 || ACADEMIC_CONTENT_HINTS.stream().noneMatch(combined::contains);
    }

    private boolean looksProtected(Document document) {
        String text = (document.title() + " " + document.text()).toLowerCase(Locale.ROOT);
        return PROTECTED_PAGE_HINTS.stream().anyMatch(text::contains);
    }

    private String extractVisibleBodyText(Document document) {
        Document clone = document.clone();
        clone.select("script,style,noscript,svg,canvas,iframe,header,footer,nav,form,.cookie,.breadcrumbs,.menu,.modal,.search,.newsletter,.social,.share,.advert,.ad").remove();
        Element body = clone.body();
        if (body == null) {
            return "";
        }
        return truncate(body.text(), MAX_CHARS_PER_PAGE);
    }

    private List<String> extractHeadings(Document document) {
        List<String> headings = new ArrayList<>();
        for (Element heading : document.select("h1, h2, h3")) {
            String text = truncate(heading.text(), 180);
            if (!text.isBlank() && headings.stream().noneMatch(text::equalsIgnoreCase)) {
                headings.add(text);
            }
            if (headings.size() >= MAX_HEADINGS) {
                break;
            }
        }
        return headings;
    }

    private Document connect(String url) throws IOException {
        Connection connection = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("Accept", ACCEPT)
                .timeout(crawl().getTimeoutMs())
                .followRedirects(true)
                .ignoreContentType(false);
        return connection.get();
    }

    private boolean isSameHost(String baseUrl, String candidateUrl) {
        try {
            URI base = URI.create(baseUrl);
            URI candidate = URI.create(candidateUrl);
            return base.getHost() != null && base.getHost().equalsIgnoreCase(candidate.getHost());
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

    private boolean isRetryable(IOException ex) {
        if (ex instanceof MalformedURLException || ex instanceof UnsupportedMimeTypeException) {
            return false;
        }
        if (ex instanceof HttpStatusException statusException) {
            int statusCode = statusException.getStatusCode();
            return statusCode >= 500 || statusCode == 429 || statusCode == 408;
        }
        return true;
    }

    private long backoffMillis(int attempt) {
        long factor = 1L << (attempt - 1);
        return INITIAL_BACKOFF.toMillis() * factor;
    }

    private UniversityCrawlFailureType classifyFailure(IOException ex) {
        if (ex == null) return UniversityCrawlFailureType.FETCH_ERROR;
        if (ex instanceof SocketTimeoutException) return UniversityCrawlFailureType.TIMEOUT;
        if (ex instanceof SSLException) return UniversityCrawlFailureType.SSL_ERROR;
        if (ex instanceof HttpStatusException statusException) {
            int code = statusException.getStatusCode();
            if (code == 403 || code == 401) return UniversityCrawlFailureType.FORBIDDEN;
            if (code == 404) return UniversityCrawlFailureType.NOT_FOUND;
        }
        return UniversityCrawlFailureType.NETWORK_ERROR;
    }

    private String friendlyFailure(UniversityCrawlFailureType failureType) {
        return switch (failureType) {
            case TIMEOUT -> "Source timed out before academic content could be verified.";
            case FORBIDDEN, PROTECTED -> "Source blocked automated access or requires login.";
            case NOT_FOUND -> "Source URL could not be verified.";
            case NO_RESULT, THIN_CONTENT, IRRELEVANT_CONTENT -> "Source did not provide usable academic content.";
            case SSL_ERROR, NETWORK_ERROR, FETCH_ERROR, ACCESS_DENIED -> "Source could not be fetched reliably.";
            default -> "Source could not be verified.";
        };
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
