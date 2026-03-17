package com.edurite.ai.university;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UniversityCrawlOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(UniversityCrawlOrchestrator.class);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<String> QUALIFICATION_LEVEL_KEYWORDS = Set.of(
            "undergraduate", "postgraduate", "diploma", "degree", "bachelor", "master", "phd", "doctoral", "certificate"
    );

    private final UniversitySourceRegistryService registryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final CrawledUniversityPageRepository repository;

    public UniversityCrawlOrchestrator(UniversitySourceRegistryService registryService,
                                       MultiUniversityPageFetcherService pageFetcherService,
                                       CrawledUniversityPageRepository repository) {
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.repository = repository;
    }

    public UniversityCrawlSummary crawlAllActiveUniversities() {
        long startedAt = System.currentTimeMillis();
        int universitiesProcessed = 0;
        int seedUrlsProcessed = 0;
        int pagesDiscovered = 0;
        int pagesSaved = 0;
        int failures = 0;

        for (UniversityRegistryProperties.UniversityRegistryEntry university : registryService.getActiveUniversities()) {
            universitiesProcessed++;
            try {
                List<String> discovered = pageFetcherService.discoverCandidateUrls(university, Integer.MAX_VALUE);
                seedUrlsProcessed += university.getSeedUrls().size();
                pagesDiscovered += discovered.size();

                for (UniversitySourcePageResult page : pageFetcherService.fetchPages(discovered)) {
                    pagesSaved++;
                    upsert(university.getUniversityName(), page);
                    if (!page.success()) {
                        failures++;
                    }
                }
            } catch (Exception ex) {
                failures++;
                log.warn("Crawl failed for university={}: {}", university.getUniversityName(), ex.getMessage());
            }
        }

        return new UniversityCrawlSummary(universitiesProcessed, seedUrlsProcessed, pagesDiscovered,
                pagesSaved, failures, System.currentTimeMillis() - startedAt);
    }

    private void upsert(String universityName, UniversitySourcePageResult pageResult) {
        CrawledUniversityPage page = repository.findBySourceUrl(pageResult.sourceUrl()).orElseGet(CrawledUniversityPage::new);
        OffsetDateTime now = OffsetDateTime.now();
        page.setUniversityName(universityName);
        page.setSourceUrl(pageResult.sourceUrl());
        page.setPageTitle(pageResult.pageTitle());
        page.setPageType(pageResult.pageType().name());
        page.setExtractedKeywords(pageResult.extractedKeywords());
        page.setLastCrawledAt(now);

        String normalizedContent = normalize(pageResult.cleanedText());
        String newContentHash = normalizedContent.isBlank() ? null : DigestUtils.sha256Hex(normalizedContent);
        if (pageResult.success() && newContentHash != null && newContentHash.equals(page.getContentHash())) {
            // Content did not change, so we only refresh crawl timestamps and status fields.
            page.setCrawlStatus(CrawlStatus.SUCCESS);
            page.setLastSuccessfulCrawledAt(now);
            page.setFailureReason(null);
            page.setErrorType(null);
            page.setLastFailureAt(null);
            repository.save(page);
            return;
        }

        page.setCleanedContent(pageResult.cleanedText());
        page.setSummaryExcerpt(buildSummary(pageResult.cleanedText()));
        page.setContentHash(newContentHash);
        page.setQualificationLevel(extractQualificationLevel(pageResult));
        page.setFacultyName(extractMetadataValue(pageResult, "faculty"));
        page.setCampusName(extractMetadataValue(pageResult, "campus"));

        if (pageResult.success()) {
            page.setCrawlStatus(CrawlStatus.SUCCESS);
            page.setLastSuccessfulCrawledAt(now);
            page.setFailureReason(null);
            page.setErrorType(null);
            page.setLastFailureAt(null);
        } else {
            page.setCrawlStatus(CrawlStatus.FAILED);
            page.setFailureReason(pageResult.failureReason());
            page.setErrorType(pageResult.failureType() == null ? UniversityCrawlFailureType.FETCH_ERROR.name() : pageResult.failureType().name());
            page.setLastFailureAt(now);
        }
        repository.save(page);
    }

    private String extractQualificationLevel(UniversitySourcePageResult pageResult) {
        String combined = normalize(pageResult.pageTitle()) + " " + normalize(pageResult.cleanedText()) + " "
                + pageResult.extractedKeywords().stream().map(this::normalize).collect(Collectors.joining(" "));
        if (combined.isBlank()) {
            return null;
        }
        for (String keyword : QUALIFICATION_LEVEL_KEYWORDS) {
            if (combined.contains(keyword)) {
                return switch (keyword) {
                    case "undergraduate", "bachelor", "diploma", "certificate" -> "Undergraduate";
                    case "postgraduate", "master", "phd", "doctoral" -> "Postgraduate";
                    default -> "Mixed";
                };
            }
        }
        return null;
    }

    private String extractMetadataValue(UniversitySourcePageResult pageResult, String marker) {
        String title = normalize(pageResult.pageTitle());
        String content = normalize(pageResult.cleanedText());
        String prefix = marker.toLowerCase(Locale.ROOT) + " ";
        if (title.contains(prefix)) {
            return clipFromMarker(title, prefix);
        }
        if (content.contains(prefix)) {
            return clipFromMarker(content, prefix);
        }
        return null;
    }

    private String clipFromMarker(String source, String marker) {
        int index = source.indexOf(marker);
        if (index < 0) {
            return null;
        }
        String candidate = source.substring(index + marker.length()).trim();
        if (candidate.isBlank()) {
            return null;
        }
        int maxLength = Math.min(candidate.length(), 80);
        return candidate.substring(0, maxLength);
    }

    private String buildSummary(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.length() <= 320 ? normalized : normalized.substring(0, 320);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return WHITESPACE.matcher(value).replaceAll(" ").trim();
    }
}
