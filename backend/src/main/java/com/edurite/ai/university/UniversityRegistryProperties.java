package com.edurite.ai.university;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "edurite.university")
public class UniversityRegistryProperties {

    private List<UniversityRegistryEntry> registry = new ArrayList<>();
    private CrawlProperties crawl = new CrawlProperties();

    public List<UniversityRegistryEntry> getRegistry() {
        return registry;
    }

    public void setRegistry(List<UniversityRegistryEntry> registry) {
        this.registry = registry;
    }

    public CrawlProperties getCrawl() {
        return crawl;
    }

    public void setCrawl(CrawlProperties crawl) {
        this.crawl = crawl;
    }

    public static class CrawlProperties {

        private int maxDiscoveredCandidatesPerUniversity = 12;
        private int maxFetchedPagesPerUniversity = 8;
        private int maxSuccessfulPagesPerUniversity = 5;
        private int maxFailedPagesPerUniversity = 3;
        private int maxCandidateLinksPerPage = 20;
        private int timeoutMs = 8_000;
        private int maxFetchRetries = 2;
        private int politeDelayMs = 150;

        public int getMaxDiscoveredCandidatesPerUniversity() {
            return maxDiscoveredCandidatesPerUniversity;
        }

        public void setMaxDiscoveredCandidatesPerUniversity(int maxDiscoveredCandidatesPerUniversity) {
            this.maxDiscoveredCandidatesPerUniversity = maxDiscoveredCandidatesPerUniversity;
        }

        public int getMaxFetchedPagesPerUniversity() {
            return maxFetchedPagesPerUniversity;
        }

        public void setMaxFetchedPagesPerUniversity(int maxFetchedPagesPerUniversity) {
            this.maxFetchedPagesPerUniversity = maxFetchedPagesPerUniversity;
        }

        public int getMaxSuccessfulPagesPerUniversity() {
            return maxSuccessfulPagesPerUniversity;
        }

        public void setMaxSuccessfulPagesPerUniversity(int maxSuccessfulPagesPerUniversity) {
            this.maxSuccessfulPagesPerUniversity = maxSuccessfulPagesPerUniversity;
        }

        public int getMaxFailedPagesPerUniversity() {
            return maxFailedPagesPerUniversity;
        }

        public void setMaxFailedPagesPerUniversity(int maxFailedPagesPerUniversity) {
            this.maxFailedPagesPerUniversity = maxFailedPagesPerUniversity;
        }

        public int getMaxCandidateLinksPerPage() {
            return maxCandidateLinksPerPage;
        }

        public void setMaxCandidateLinksPerPage(int maxCandidateLinksPerPage) {
            this.maxCandidateLinksPerPage = maxCandidateLinksPerPage;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getMaxFetchRetries() {
            return maxFetchRetries;
        }

        public void setMaxFetchRetries(int maxFetchRetries) {
            this.maxFetchRetries = maxFetchRetries;
        }

        public int getPoliteDelayMs() {
            return politeDelayMs;
        }

        public void setPoliteDelayMs(int politeDelayMs) {
            this.politeDelayMs = politeDelayMs;
        }
    }

    public static class UniversityRegistryEntry {

        @NotBlank
        private String universityName;
        @NotBlank
        private String baseDomain;
        private List<String> allowedDomains = new ArrayList<>();
        private List<String> officialHomepages = new ArrayList<>();
        private List<String> programmePages = new ArrayList<>();
        private List<String> admissionsPages = new ArrayList<>();
        private List<String> facultyPages = new ArrayList<>();
        private List<String> discoveryPages = new ArrayList<>();
        private List<String> sitemapUrls = new ArrayList<>();
        private List<String> blockedPatterns = new ArrayList<>();
        private List<String> allowedPatterns = new ArrayList<>();
        private List<String> qualificationLevelsSupported = new ArrayList<>();
        private boolean active = true;
        private int crawlPriority = 100;
        private int maxPagesToFetch = 8;
        private String sourcePriority = SourcePriority.STANDARD.name();

        public String getUniversityName() { return universityName; }
        public void setUniversityName(String universityName) { this.universityName = universityName; }
        public String getBaseDomain() { return baseDomain; }
        public void setBaseDomain(String baseDomain) { this.baseDomain = baseDomain; }
        public List<String> getAllowedDomains() { return allowedDomains; }
        public void setAllowedDomains(List<String> allowedDomains) { this.allowedDomains = allowedDomains; }
        public List<String> getOfficialHomepages() { return officialHomepages; }
        public void setOfficialHomepages(List<String> officialHomepages) { this.officialHomepages = officialHomepages; }
        public List<String> getProgrammePages() { return programmePages; }
        public void setProgrammePages(List<String> programmePages) { this.programmePages = programmePages; }
        public List<String> getAdmissionsPages() { return admissionsPages; }
        public void setAdmissionsPages(List<String> admissionsPages) { this.admissionsPages = admissionsPages; }
        public List<String> getFacultyPages() { return facultyPages; }
        public void setFacultyPages(List<String> facultyPages) { this.facultyPages = facultyPages; }
        public List<String> getDiscoveryPages() { return discoveryPages; }
        public void setDiscoveryPages(List<String> discoveryPages) { this.discoveryPages = discoveryPages; }
        public List<String> getSitemapUrls() { return sitemapUrls; }
        public void setSitemapUrls(List<String> sitemapUrls) { this.sitemapUrls = sitemapUrls; }
        public List<String> getBlockedPatterns() { return blockedPatterns; }
        public void setBlockedPatterns(List<String> blockedPatterns) { this.blockedPatterns = blockedPatterns; }
        public List<String> getAllowedPatterns() { return allowedPatterns; }
        public void setAllowedPatterns(List<String> allowedPatterns) { this.allowedPatterns = allowedPatterns; }
        public List<String> getQualificationLevelsSupported() { return qualificationLevelsSupported; }
        public void setQualificationLevelsSupported(List<String> qualificationLevelsSupported) { this.qualificationLevelsSupported = qualificationLevelsSupported; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public int getCrawlPriority() { return crawlPriority; }
        public void setCrawlPriority(int crawlPriority) { this.crawlPriority = crawlPriority; }
        public int getMaxPagesToFetch() { return maxPagesToFetch; }
        public void setMaxPagesToFetch(int maxPagesToFetch) { this.maxPagesToFetch = maxPagesToFetch; }
        public String getSourcePriority() { return sourcePriority; }
        public void setSourcePriority(String sourcePriority) { this.sourcePriority = sourcePriority; }

        public List<String> getSeedUrls() {
            Set<String> urls = new LinkedHashSet<>();
            urls.addAll(officialHomepages);
            urls.addAll(programmePages);
            urls.addAll(admissionsPages);
            urls.addAll(facultyPages);
            urls.addAll(discoveryPages);
            return new ArrayList<>(urls);
        }

        public void setSeedUrls(List<String> seedUrls) {
            this.officialHomepages = seedUrls == null ? new ArrayList<>() : new ArrayList<>(seedUrls);
        }
    }
}
