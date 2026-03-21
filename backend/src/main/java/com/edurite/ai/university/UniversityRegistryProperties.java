package com.edurite.ai.university;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
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

        private List<String> candidatePaths = new ArrayList<>(List.of(
                "/programmes", "/programs", "/study", "/studies", "/courses", "/course-finder",
                "/undergraduate", "/postgraduate", "/faculties", "/admissions", "/academic-programmes",
                "/qualifications", "/prospectus", "/fees", "/financial-aid", "/funding", "/apply"
        ));

        private int maxDiscoveredCandidatesPerUniversity = 60;
        private int maxFetchedPagesPerUniversity = 30;
        private int maxCrawlDepth = 1;
        private int timeoutMs = 8_000;
        private int maxFetchRetries = 3;

        public List<String> getCandidatePaths() {
            return candidatePaths;
        }

        public void setCandidatePaths(List<String> candidatePaths) {
            this.candidatePaths = candidatePaths;
        }

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

        public int getMaxCrawlDepth() {
            return maxCrawlDepth;
        }

        public void setMaxCrawlDepth(int maxCrawlDepth) {
            this.maxCrawlDepth = maxCrawlDepth;
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
    }

    public static class UniversityRegistryEntry {

        @NotBlank
        private String institutionKey;
        @NotBlank
        private String universityName;
        private String institutionType = "PUBLIC_UNIVERSITY";
        private String countryCode = "ZA";
        private String province = "";
        @NotBlank
        private String baseDomain;
        private List<String> allowedDomains = new ArrayList<>();
        private List<String> officialHomepages = new ArrayList<>();
        private List<String> admissionsPages = new ArrayList<>();
        private List<String> programmePages = new ArrayList<>();
        private List<String> facultyPages = new ArrayList<>();
        private List<String> prospectusPages = new ArrayList<>();
        private List<String> discoveryPages = new ArrayList<>();
        private List<String> allowedPatterns = new ArrayList<>();
        private List<String> blockedPatterns = new ArrayList<>();
        private List<String> seedUrls = new ArrayList<>();
        private List<String> qualificationLevelsSupported = new ArrayList<>();
        private boolean active = true;
        private boolean enabled = true;
        private int crawlPriority = 100;
        private int maxPagesToFetch = 12;

        public String getInstitutionKey() {
            return institutionKey == null || institutionKey.isBlank() ? universityName : institutionKey;
        }

        public void setInstitutionKey(String institutionKey) {
            this.institutionKey = institutionKey;
        }

        public String getUniversityName() {
            return universityName;
        }

        public void setUniversityName(String universityName) {
            this.universityName = universityName;
        }

        public String getInstitutionType() {
            return institutionType;
        }

        public void setInstitutionType(String institutionType) {
            this.institutionType = institutionType;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getBaseDomain() {
            return baseDomain;
        }

        public void setBaseDomain(String baseDomain) {
            this.baseDomain = baseDomain;
        }

        public List<String> getAllowedDomains() {
            return allowedDomains;
        }

        public void setAllowedDomains(List<String> allowedDomains) {
            this.allowedDomains = allowedDomains;
        }

        public List<String> getOfficialHomepages() {
            return officialHomepages;
        }

        public void setOfficialHomepages(List<String> officialHomepages) {
            this.officialHomepages = officialHomepages;
        }

        public List<String> getAdmissionsPages() {
            return admissionsPages;
        }

        public void setAdmissionsPages(List<String> admissionsPages) {
            this.admissionsPages = admissionsPages;
        }

        public List<String> getProgrammePages() {
            return programmePages;
        }

        public void setProgrammePages(List<String> programmePages) {
            this.programmePages = programmePages;
        }

        public List<String> getFacultyPages() {
            return facultyPages;
        }

        public void setFacultyPages(List<String> facultyPages) {
            this.facultyPages = facultyPages;
        }

        public List<String> getProspectusPages() {
            return prospectusPages;
        }

        public void setProspectusPages(List<String> prospectusPages) {
            this.prospectusPages = prospectusPages;
        }

        public List<String> getDiscoveryPages() {
            return discoveryPages;
        }

        public void setDiscoveryPages(List<String> discoveryPages) {
            this.discoveryPages = discoveryPages;
        }

        public List<String> getAllowedPatterns() {
            return allowedPatterns;
        }

        public void setAllowedPatterns(List<String> allowedPatterns) {
            this.allowedPatterns = allowedPatterns;
        }

        public List<String> getBlockedPatterns() {
            return blockedPatterns;
        }

        public void setBlockedPatterns(List<String> blockedPatterns) {
            this.blockedPatterns = blockedPatterns;
        }

        public List<String> getSeedUrls() {
            return seedUrls;
        }

        public void setSeedUrls(List<String> seedUrls) {
            this.seedUrls = seedUrls;
        }

        public List<String> getQualificationLevelsSupported() {
            return qualificationLevelsSupported;
        }

        public void setQualificationLevelsSupported(List<String> qualificationLevelsSupported) {
            this.qualificationLevelsSupported = qualificationLevelsSupported;
        }

        public boolean isActive() {
            return active && enabled;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCrawlPriority() {
            return crawlPriority;
        }

        public void setCrawlPriority(int crawlPriority) {
            this.crawlPriority = crawlPriority;
        }

        public int getMaxPagesToFetch() {
            return maxPagesToFetch;
        }

        public void setMaxPagesToFetch(int maxPagesToFetch) {
            this.maxPagesToFetch = maxPagesToFetch;
        }

        public List<String> getAllEntryPoints() {
            List<String> urls = new ArrayList<>();
            urls.addAll(officialHomepages);
            urls.addAll(admissionsPages);
            urls.addAll(programmePages);
            urls.addAll(facultyPages);
            urls.addAll(prospectusPages);
            urls.addAll(discoveryPages);
            urls.addAll(seedUrls);
            return urls;
        }
    }
}
