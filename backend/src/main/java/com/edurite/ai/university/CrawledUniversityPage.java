package com.edurite.ai.university;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "crawled_university_pages", indexes = {
        @Index(name = "idx_crawled_pages_university", columnList = "university_name"),
        @Index(name = "idx_crawled_pages_status", columnList = "crawl_status"),
        @Index(name = "idx_crawled_pages_last_crawled", columnList = "last_crawled_at")
})
public class CrawledUniversityPage extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String universityName;

    @Column(nullable = false, unique = true, length = 1200)
    private String sourceUrl;

    @Column(length = 500)
    private String pageTitle;

    @Column(length = 80)
    private String pageType;

    @Column(length = 80)
    private String qualificationLevel;

    @Column(length = 180)
    private String facultyName;

    @Column(length = 180)
    private String campusName;

    @ElementCollection
    @CollectionTable(name = "crawled_university_page_keywords", joinColumns = @JoinColumn(name = "page_id"))
    @Column(name = "keyword", length = 120)
    private Set<String> extractedKeywords = new LinkedHashSet<>();

    @Column(columnDefinition = "TEXT")
    private String cleanedContent;

    @Column(length = 128)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrawlStatus crawlStatus = CrawlStatus.SKIPPED;

    private OffsetDateTime lastCrawledAt;
    private OffsetDateTime lastSuccessfulCrawledAt;
    private OffsetDateTime lastFailureAt;

    @Column(length = 100)
    private String errorType;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String summaryExcerpt;

    public String getUniversityName() { return universityName; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getPageTitle() { return pageTitle; }
    public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }
    public String getPageType() { return pageType; }
    public void setPageType(String pageType) { this.pageType = pageType; }
    public String getQualificationLevel() { return qualificationLevel; }
    public void setQualificationLevel(String qualificationLevel) { this.qualificationLevel = qualificationLevel; }
    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
    public String getCampusName() { return campusName; }
    public void setCampusName(String campusName) { this.campusName = campusName; }
    public Set<String> getExtractedKeywords() { return extractedKeywords; }
    public void setExtractedKeywords(Set<String> extractedKeywords) { this.extractedKeywords = extractedKeywords; }
    public String getCleanedContent() { return cleanedContent; }
    public void setCleanedContent(String cleanedContent) { this.cleanedContent = cleanedContent; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public CrawlStatus getCrawlStatus() { return crawlStatus; }
    public void setCrawlStatus(CrawlStatus crawlStatus) { this.crawlStatus = crawlStatus; }
    public OffsetDateTime getLastCrawledAt() { return lastCrawledAt; }
    public void setLastCrawledAt(OffsetDateTime lastCrawledAt) { this.lastCrawledAt = lastCrawledAt; }
    public OffsetDateTime getLastSuccessfulCrawledAt() { return lastSuccessfulCrawledAt; }
    public void setLastSuccessfulCrawledAt(OffsetDateTime lastSuccessfulCrawledAt) { this.lastSuccessfulCrawledAt = lastSuccessfulCrawledAt; }
    public OffsetDateTime getLastFailureAt() { return lastFailureAt; }
    public void setLastFailureAt(OffsetDateTime lastFailureAt) { this.lastFailureAt = lastFailureAt; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getSummaryExcerpt() { return summaryExcerpt; }
    public void setSummaryExcerpt(String summaryExcerpt) { this.summaryExcerpt = summaryExcerpt; }
}
