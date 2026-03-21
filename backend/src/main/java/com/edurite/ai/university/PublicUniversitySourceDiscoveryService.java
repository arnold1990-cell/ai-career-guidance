package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class PublicUniversitySourceDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(PublicUniversitySourceDiscoveryService.class);

    private static final String SEARCH_ENDPOINT = "https://html.duckduckgo.com/html/?q=";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EduRiteDiscovery/1.0; +https://edurite.ai/bot)";
    private static final int MAX_SEARCH_RESULTS_PER_QUERY = 8;

    private final UniversitySourceRegistryService registryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final UniversityUrlNormalizer urlNormalizer;

    public PublicUniversitySourceDiscoveryService(UniversitySourceRegistryService registryService,
                                                  MultiUniversityPageFetcherService pageFetcherService,
                                                  UniversityUrlNormalizer urlNormalizer) {
        this.registryService = registryService;
        this.pageFetcherService = pageFetcherService;
        this.urlNormalizer = urlNormalizer;
    }

    public List<String> discoverSources(StudentProfile profile, UniversitySourcesAnalysisRequest request, int maxUrls) {
        List<UniversityRegistryProperties.UniversityRegistryEntry> rankedUniversities = rankedUniversities(profile, request);
        Set<String> discovered = new LinkedHashSet<>();
        log.info("University source discovery starting: registrySize={}, rankedUniversities={}, requestedSources={}, usesDefaultSources={}",
                registryService.configuredUniversityCount(), rankedUniversities.size(), maxUrls, request.usesDefaultSources());

        for (UniversityRegistryProperties.UniversityRegistryEntry university : rankedUniversities) {
            discovered.addAll(university.getSeedUrls());
            List<String> candidateUrls = pageFetcherService.discoverCandidateUrls(university, Math.max(4, maxUrls / 2));
            if (candidateUrls.isEmpty()) {
                candidateUrls = fallbackToHomepages(university);
                log.warn("University discovery produced no crawler candidates; using homepage fallback: university={}, fallbackUrls={}",
                        university.getUniversityName(), candidateUrls.size());
            }
            discovered.addAll(candidateUrls);
            discovered.addAll(searchOfficialPages(university, request, profile));
            if (discovered.size() >= maxUrls) {
                break;
            }
        }

        List<String> normalized = discovered.stream()
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .filter(registryService::isAllowedUrl)
                .limit(maxUrls)
                .toList();

        if (normalized.isEmpty()) {
            normalized = registryService.getFallbackSources(maxUrls);
            log.warn("University source discovery returned zero URLs after filtering; falling back to registry seed URLs: fallbackUrls={}", normalized.size());
        }

        log.info("University source discovery completed: registrySize={}, rankedUniversities={}, discoveredUrls={}, requestedSources={}",
                registryService.configuredUniversityCount(), rankedUniversities.size(), normalized.size(), maxUrls);
        return normalized;
    }


    private List<String> fallbackToHomepages(UniversityRegistryProperties.UniversityRegistryEntry university) {
        return university.getSeedUrls().stream()
                .map(urlNormalizer::normalize)
                .filter(url -> !url.isBlank())
                .toList();
    }

    private List<UniversityRegistryProperties.UniversityRegistryEntry> rankedUniversities(StudentProfile profile,
                                                                                           UniversitySourcesAnalysisRequest request) {
        return registryService.getActiveUniversities().stream()
                .sorted(Comparator.comparingInt((UniversityRegistryProperties.UniversityRegistryEntry entry) -> relevanceScore(entry, profile, request)).reversed())
                .limit(Math.min(Math.max(1, request.safeMaxRecommendations() * 4), registryService.configuredUniversityCount()))
                .toList();
    }

    private int relevanceScore(UniversityRegistryProperties.UniversityRegistryEntry university,
                               StudentProfile profile,
                               UniversitySourcesAnalysisRequest request) {
        String text = (university.getUniversityName() + " " + university.getBaseDomain() + " "
                + String.join(" ", university.getQualificationLevelsSupported())).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : tokens(profile, request)) {
            if (text.contains(token)) {
                score += 2;
            }
        }
        if (request.qualificationLevel() != null && university.getQualificationLevelsSupported().stream()
                .anyMatch(level -> level.equalsIgnoreCase(request.qualificationLevel()))) {
            score += 4;
        }
        return score;
    }

    private List<String> searchOfficialPages(UniversityRegistryProperties.UniversityRegistryEntry university,
                                             UniversitySourcesAnalysisRequest request,
                                             StudentProfile profile) {
        Set<String> results = new LinkedHashSet<>();
        for (String query : buildQueries(university, request, profile)) {
            try {
                Document document = Jsoup.connect(SEARCH_ENDPOINT + URLEncoder.encode(query, StandardCharsets.UTF_8))
                        .userAgent(USER_AGENT)
                        .timeout(10_000)
                        .get();
                int count = 0;
                for (Element link : document.select("a.result__a")) {
                    String url = urlNormalizer.normalize(link.absUrl("href"));
                    if (url.isBlank()) {
                        url = urlNormalizer.normalize(link.attr("href"));
                    }
                    if (!registryService.isAllowedUrlForUniversity(university.getUniversityName(), url)) {
                        continue;
                    }
                    if (!looksUseful(url, link.text())) {
                        continue;
                    }
                    results.add(url);
                    count++;
                    if (count >= MAX_SEARCH_RESULTS_PER_QUERY) {
                        break;
                    }
                }
            } catch (IOException ex) {
                log.warn("University search discovery failed for university={}, query={}, message={}",
                        university.getUniversityName(), query, ex.getMessage());
            }
        }
        return List.copyOf(results);
    }

    private List<String> buildQueries(UniversityRegistryProperties.UniversityRegistryEntry university,
                                      UniversitySourcesAnalysisRequest request,
                                      StudentProfile profile) {
        String programme = compact(request.targetProgram());
        String career = compact(request.careerInterest());
        String qualification = compact(request.qualificationLevel());
        String location = compact(profile.getLocation());
        String domain = university.getBaseDomain();
        List<String> queries = new ArrayList<>();
        queries.add("site:" + domain + " " + university.getUniversityName() + " admissions " + programme + " " + qualification);
        queries.add("site:" + domain + " " + university.getUniversityName() + " programme entry requirements " + programme);
        queries.add("site:" + domain + " " + university.getUniversityName() + " faculty department " + career + " " + location);
        return queries;
    }

    private boolean looksUseful(String url, String anchorText) {
        String text = (url + " " + anchorText).toLowerCase(Locale.ROOT);
        if (text.contains("login") || text.contains("sign-in") || text.contains("portal")) {
            return false;
        }
        for (String keyword : List.of("admission", "programme", "program", "faculty", "department", "requirements", "undergraduate", "postgraduate", "study", "course")) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> tokens(StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        Map<String, Boolean> values = new LinkedHashMap<>();
        addTokens(values, request.targetProgram());
        addTokens(values, request.careerInterest());
        addTokens(values, request.qualificationLevel());
        addTokens(values, profile.getInterests());
        addTokens(values, profile.getSkills());
        addTokens(values, profile.getLocation());
        return new ArrayList<>(values.keySet());
    }

    private void addTokens(Map<String, Boolean> values, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String token : text.toLowerCase(Locale.ROOT).split("[,\\s/]+")) {
            if (token.length() >= 3) {
                values.put(token, Boolean.TRUE);
            }
        }
    }

    private String compact(String value) {
        return value == null ? "" : value.trim();
    }
}
