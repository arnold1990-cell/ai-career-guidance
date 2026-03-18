package com.edurite.ai.university;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.student.entity.StudentProfile;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class PublicUniversitySourceDiscoveryService {

    private static final String SEARCH_ENDPOINT = "https://html.duckduckgo.com/html/?q=";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; EduRiteSearchBot/1.0; +https://edurite.ai/bot)";
    private static final int SEARCH_TIMEOUT_MS = 10_000;

    private final UniversitySourceRegistryService registryService;

    public PublicUniversitySourceDiscoveryService(UniversitySourceRegistryService registryService) {
        this.registryService = registryService;
    }

    public List<String> discoverPublicUniversityUrls(StudentProfile profile,
                                                     UniversitySourcesAnalysisRequest request,
                                                     int limit) {
        Set<String> discovered = new LinkedHashSet<>();
        List<String> queries = buildQueries(profile, request);
        for (String query : queries) {
            if (discovered.size() >= limit) {
                break;
            }
            discovered.addAll(search(query, limit - discovered.size()));
        }
        if (discovered.isEmpty()) {
            discovered.addAll(registryService.getDefaultSources().stream().limit(limit).toList());
        }
        return discovered.stream().limit(limit).toList();
    }

    private List<String> buildQueries(StudentProfile profile, UniversitySourcesAnalysisRequest request) {
        List<String> focusTerms = new ArrayList<>();
        addTerm(focusTerms, request.targetProgram());
        addTerm(focusTerms, request.careerInterest());
        addTerm(focusTerms, request.qualificationLevel());
        addTerm(focusTerms, profile.getLocation());
        String focus = String.join(" ", focusTerms).trim();

        return registryService.getActiveUniversities().stream()
                .limit(8)
                .flatMap(university -> List.of(
                        focus + " site:" + university.getBaseDomain() + " programme admission requirements",
                        focus + " site:" + university.getBaseDomain() + " faculty course",
                        focus + " site:" + university.getBaseDomain() + " undergraduate postgraduate"
                ).stream())
                .distinct()
                .limit(24)
                .toList();
    }

    private List<String> search(String query, int remainingLimit) {
        try {
            Document document = Jsoup.connect(SEARCH_ENDPOINT + java.net.URLEncoder.encode(query, StandardCharsets.UTF_8))
                    .userAgent(USER_AGENT)
                    .timeout(SEARCH_TIMEOUT_MS)
                    .get();

            return document.select("a[href]").stream()
                    .map(link -> extractTargetUrl(link.attr("href")))
                    .filter(url -> !url.isBlank())
                    .filter(registryService::isAllowedUrl)
                    .filter(this::looksLikeUsefulUniversityPage)
                    .sorted(Comparator.comparingInt(this::pageScore).reversed())
                    .distinct()
                    .limit(remainingLimit)
                    .toList();
        } catch (IOException ex) {
            return List.of();
        }
    }

    private String extractTargetUrl(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        int start = href.indexOf("uddg=");
        if (start < 0) {
            return "";
        }
        return URLDecoder.decode(href.substring(start + 5), StandardCharsets.UTF_8);
    }

    private boolean looksLikeUsefulUniversityPage(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("login") || lower.contains("signin") || lower.contains("directory") || lower.contains("search")) {
            return false;
        }
        return lower.contains("programme") || lower.contains("program") || lower.contains("course")
                || lower.contains("faculty") || lower.contains("admission") || lower.contains("requirement")
                || lower.contains("undergraduate") || lower.contains("postgraduate");
    }

    private int pageScore(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        int score = 0;
        if (lower.contains("admission")) score += 5;
        if (lower.contains("programme") || lower.contains("program")) score += 5;
        if (lower.contains("requirement")) score += 4;
        if (lower.contains("faculty")) score += 3;
        if (lower.contains("undergraduate") || lower.contains("postgraduate")) score += 2;
        return score;
    }

    private void addTerm(List<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }
}
