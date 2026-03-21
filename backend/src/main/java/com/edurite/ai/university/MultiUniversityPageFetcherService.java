package com.edurite.ai.university;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MultiUniversityPageFetcherService {

    private static final Logger log = LoggerFactory.getLogger(MultiUniversityPageFetcherService.class);

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EduRiteCrawler/1.0; +https://edurite.ai/bot)";
    private static final String ACCEPT_LANGUAGE = "en-ZA,en;q=0.9";
    private static final int MAX_CHARS_PER_PAGE = 8_000;
    private static final int MAX_LINKS_PER_SEED = 30;
    private static final int MAX_HEADINGS = 8;
    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(200);

    private static final Map<String, Integer> LINK_RELEVANCE_TERMS = Map.ofEntries(
            Map.entry("programme", 4),
            Map.entry("program", 4),
            Map.entry("course", 3),
            Map.entry("study", 3),
            Map.entry("faculty", 3),
            Map.entry("admission", 3),
            Map.entry("requirements", 3),
            Map.entry("undergraduate", 2),
            Map.entry("postgraduate", 2),
            Map.entry("qualification", 2),
            Map.entry("degree", 2),
            Map.entry("diploma", 2),
            Map.entry("prospectus", 2),
            Map.entry("fees", 2),
            Map.entry("financial-aid", 2),
            Map.entry("bursary", 2)
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
            if (crawl().getMaxCrawlDepth() > 0) {
                visited.add(normalizedSeed);
                discovered.addAll(extractRankedInternalLinks(university, normalizedSeed, cappedMaxUrls, visited));
            }
            if (discovered.size() >= cappedMaxUrls) {
                break;
            }
        }

        List<String> results = discovered.stream()
                .sorted(Comparator.comparingInt(this::candidatePriority).reversed())
                .limit(cappedMaxUrls)
                .toList();
        log.info("University crawler discovery completed: university={}, seedUrls={}, discoveredUrls={}, requestedSources={}",
                university.getUniversityName(), university.getSeedUrls().size(), results.size(), cappedMaxUrls);
        return results;
    }

    public List<UniversitySourcePageResult> fetchPages(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }

        List<String> normalizedUrls = urls.stream()
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .distinct()
                .toList();

        Map<String, Deque<String>> urlsByUniversity = new LinkedHashMap<>();
        for (String url : normalizedUrls) {
            String universityKey = registryService.resolveUniversityName(url).orElseGet(() -> hostKey(url));
            urlsByUniversity.computeIfAbsent(universityKey, ignored -> new ArrayDeque<>()).add(url);
        }

        int maxFetchesPerUniversity = Math.max(1, crawl().getMaxFetchedPagesPerUniversity());
        Map<String, Integer> fetchCounts = new LinkedHashMap<>();
        Map<String, UniversitySourcePageResult> cache = new LinkedHashMap<>();
        List<UniversitySourcePageResult> results = new ArrayList<>();
        boolean progressed;

        do {
            progressed = false;
            for (Map.Entry<String, Deque<String>> entry : urlsByUniversity.entrySet()) {
                String universityKey = entry.getKey();
                Deque<String> universityUrls = entry.getValue();
                if (universityUrls.isEmpty()) {
                    continue;
                }
                int currentCount = fetchCounts.getOrDefault(universityKey, 0);
                if (currentCount >= maxFetchesPerUniversity) {
                    universityUrls.clear();
                    continue;
                }
                String url = universityUrls.pollFirst();
                if (url == null) {
                    continue;
                }
                progressed = true;
                UniversitySourcePageResult result = cache.computeIfAbsent(url, this::fetchAllowlistedSingle);
                results.add(result);
                fetchCounts.put(universityKey, currentCount + 1);
            }
        } while (progressed);

        log.info("University page fetch batching completed: requestedUrls={}, universities={}, fetchedPages={}, maxFetchesPerUniversity={}",
                normalizedUrls.size(), urlsByUniversity.size(), results.size(), maxFetchesPerUniversity);
        return results;
    }

    private UniversitySourcePageResult fetchAllowlistedSingle(String url) {
        if (!registryService.isAllowedUrl(url)) {
            return new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false,
                    "URL domain is not allowlisted", UniversityCrawlFailureType.ACCESS_DENIED);
        }
        return fetchSingle(url);
    }

    private UniversitySourcePageResult fetchSingle(String url) {
        IOException lastError = null;
        int maxRetries = Math.max(1, crawl().getMaxFetchRetries());
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Document document = connect(url);
                String title = truncate(document.title(), 200);
                if (looksLikeLoginPage(document)) {
                    return new UniversitySourcePageResult(url, title, UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false,
                            "Rejected login-style page", UniversityCrawlFailureType.ACCESS_DENIED);
                }
                String visibleText = extractVisibleBodyText(document);
                List<String> headings = extractHeadings(document);
                if (visibleText.isBlank()) {
                    return new UniversitySourcePageResult(url, title, UniversityPageType.UNKNOWN, "", Set.of(), headings, false,
                            "No meaningful visible body text was extracted", UniversityCrawlFailureType.EMPTY_CONTENT);
                }
                UniversityPageType pageType = classifier.classify(url, title, visibleText);
                Set<String> keywords = classifier.extractKeywords(title, visibleText);
                if (visibleText.length() < 250 && !isThinButUsefulPage(url, title, visibleText, headings, pageType, keywords) && !isFallbackAcceptablePage(url, title, visibleText, headings)) {
                    return new UniversitySourcePageResult(url, title, UniversityPageType.UNKNOWN, visibleText, Set.of(), headings, false,
                            "Visible body text was too thin for reliable grounding", UniversityCrawlFailureType.EMPTY_CONTENT);
                }
                if (classifier.shouldSkipPage(url, title, visibleText) && !isFallbackAcceptablePage(url, title, visibleText, headings)) {
                    return new UniversitySourcePageResult(url, title, pageType, visibleText, keywords, headings, false,
                            "Page was deprioritised because it does not look like a useful official programme or admissions page",
                            UniversityCrawlFailureType.FETCH_ERROR);
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
        String reason = lastError == null ? "Unknown fetch failure" : lastError.getMessage();
        return new UniversitySourcePageResult(url, "", UniversityPageType.UNKNOWN, "", Set.of(), List.of(), false, reason, classifyFailure(lastError));
    }

    private String extractVisibleBodyText(Document document) {
        Document clone = document.clone();
        clone.select("script,style,noscript,svg,canvas,iframe,header,footer,nav,form,.cookie,.breadcrumbs,.menu,.modal,.search,.newsletter,.social,.share,.advert,.ad").remove();
        Element body = clone.body();
        if (body == null) {
            return "";
        }
        String text = body.text();
        return truncate(text, MAX_CHARS_PER_PAGE);
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

    private boolean looksLikeLoginPage(Document document) {
        String text = (document.title() + " " + document.text()).toLowerCase(Locale.ROOT);
        return text.contains("sign in") || text.contains("log in") || text.contains("login") || text.contains("password");
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
                int score = relevanceScore(context) + candidatePriority(normalized);
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
            log.warn("University crawler could not extract internal links: university={}, seedUrl={}, message={}",
                    university.getUniversityName(), seedUrl, ex.getMessage());
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

    private int candidatePriority(String url) {
        String normalized = url == null ? "" : url.toLowerCase(Locale.ROOT);
        return relevanceScore(normalized)
                + (normalized.contains("undergraduate") ? 2 : 0)
                + (normalized.contains("postgraduate") ? 2 : 0)
                + (normalized.contains("admission") ? 2 : 0)
                + (normalized.contains("programme") || normalized.contains("program") ? 2 : 0)
                - (normalized.contains("news") || normalized.contains("privacy") || normalized.contains("cookie") ? 3 : 0);
    }

    private boolean isThinButUsefulPage(String url,
                                        String title,
                                        String visibleText,
                                        List<String> headings,
                                        UniversityPageType pageType,
                                        Set<String> keywords) {
        if (pageType != UniversityPageType.UNKNOWN) {
            return true;
        }
        if (!keywords.isEmpty()) {
            return true;
        }
        String headingContext = String.join(" ", headings);
        String combined = (title + " " + headingContext + " " + visibleText + " " + url).toLowerCase(Locale.ROOT);
        return combined.contains("programme")
                || combined.contains("program")
                || combined.contains("degree")
                || combined.contains("admission")
                || combined.contains("qualification")
                || combined.contains("prospectus")
                || combined.contains("fees");
    }


    private boolean isFallbackAcceptablePage(String url, String title, String visibleText, List<String> headings) {
        String combined = (url + " " + title + " " + String.join(" ", headings) + " " + visibleText).toLowerCase(Locale.ROOT);
        return combined.contains("admission")
                || combined.contains("apply")
                || combined.contains("programme")
                || combined.contains("program")
                || combined.contains("qualification")
                || combined.contains("undergraduate")
                || combined.contains("postgraduate")
                || combined.contains("fees")
                || combined.contains("financial aid")
                || combined.contains("prospectus")
                || isHomepage(url);
    }

    private boolean isHomepage(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            return path == null || path.isBlank() || "/".equals(path);
        } catch (RuntimeException ex) {
            return false;
        }
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
            return statusCode >= 500 || statusCode == 429 || statusCode == 408;
        }
        return true;
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
            return switch (statusException.getStatusCode()) {
                case 401, 403 -> UniversityCrawlFailureType.ACCESS_DENIED;
                case 404 -> UniversityCrawlFailureType.NOT_FOUND;
                case 408, 429 -> UniversityCrawlFailureType.TIMEOUT;
                default -> UniversityCrawlFailureType.FETCH_ERROR;
            };
        }
        return UniversityCrawlFailureType.FETCH_ERROR;
    }

    private long backoffMillis(int attempt) {
        return INITIAL_BACKOFF.multipliedBy((long) Math.pow(2, Math.max(0, attempt - 1))).toMillis();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String hostKey(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? "unknown-host" : uri.getHost().toLowerCase(Locale.ROOT);
        } catch (RuntimeException ex) {
            return "unknown-host";
        }
    }

    private UniversityRegistryProperties.CrawlProperties crawl() {
        return properties.getCrawl();
    }
}
