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

    public List<UniversityRegistryEntry> getRegistry() {
        return registry;
    }

    public void setRegistry(List<UniversityRegistryEntry> registry) {
        this.registry = registry;
    }

    public static class UniversityRegistryEntry {

        @NotBlank
        private String universityName;
        @NotBlank
        private String baseDomain;
        private List<String> allowedDomains = new ArrayList<>();
        private List<String> seedUrls = new ArrayList<>();
        private List<String> qualificationLevelsSupported = new ArrayList<>();
        private boolean active = true;
        private int crawlPriority = 100;

        public String getUniversityName() {
            return universityName;
        }

        public void setUniversityName(String universityName) {
            this.universityName = universityName;
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
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getCrawlPriority() {
            return crawlPriority;
        }

        public void setCrawlPriority(int crawlPriority) {
            this.crawlPriority = crawlPriority;
        }
    }
}
