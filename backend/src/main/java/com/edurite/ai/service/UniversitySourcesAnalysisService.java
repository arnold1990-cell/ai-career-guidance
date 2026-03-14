package com.edurite.ai.service;

import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.student.entity.StudentProfile;
import com.edurite.student.service.StudentService;
import com.fasterxml.jackson.databind.JsonNode;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UniversitySourcesAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(UniversitySourcesAnalysisService.class);

    private final UniversitySourceRegistryService sourceRegistryService;
    private final MultiUniversityPageFetcherService pageFetcherService;
    private final UniversitySourcesAggregatorService aggregatorService;
    private final UniversityGuidancePromptBuilder promptBuilder;
    private final GeminiService geminiService;
    private final StudentService studentService;

    public UniversitySourcesAnalysisService(UniversitySourceRegistryService sourceRegistryService,
                                            MultiUniversityPageFetcherService pageFetcherService,
                                            UniversitySourcesAggregatorService aggregatorService,
                                            UniversityGuidancePromptBuilder promptBuilder,
                                            GeminiService geminiService,
                                            StudentService studentService) {
        this.sourceRegistryService = sourceRegistryService;
        this.pageFetcherService = pageFetcherService;
        this.aggregatorService = aggregatorService;
        this.promptBuilder = promptBuilder;
        this.geminiService = geminiService;
        this.studentService = studentService;
    }

    public UniversitySourcesAnalysisResponse analyse(UniversitySourcesAnalysisRequest request, Principal principal) {
        List<String> selectedUrls = sourceRegistryService.sanitizeRequestedUrls(request.urls());
        int maxRecommendations = request.maxRecommendations() == null ? 10 : Math.max(3, Math.min(20, request.maxRecommendations()));

        StudentProfile profile = studentService.getProfileEntity(principal);
        var fetchedPages = pageFetcherService.fetchPages(selectedUrls);
        var context = aggregatorService.aggregate(
                fetchedPages,
                profile,
                request.targetProgram(),
                request.careerInterest(),
                request.qualificationLevel()
        );

        List<String> warnings = new ArrayList<>(context.warnings());

        try {
            String prompt = promptBuilder.buildPrompt(
                    profile,
                    context,
                    request.targetProgram(),
                    request.careerInterest(),
                    request.qualificationLevel(),
                    maxRecommendations,
                    seedCareers()
            );
            JsonNode json = geminiService.generateJsonNode(prompt);
            return toResponse(selectedUrls, context, warnings, json, geminiService.resolvedModelName());
        } catch (AiServiceException ex) {
            log.warn("Gemini analysis failed. Falling back to metadata-based response: {}", ex.getMessage());
            warnings.add("AI model is temporarily unavailable. Returned a safe fallback response.");
            return fallbackResponse(selectedUrls, context, warnings, geminiService.resolvedModelName());
        }
    }

    private UniversitySourcesAnalysisResponse toResponse(List<String> sourceUrls,
                                                         UniversitySourcesAggregatorService.AggregatedUniversityContext context,
                                                         List<String> warnings,
                                                         JsonNode json,
                                                         String modelName) {
        List<String> jsonWarnings = readStringList(json, "warnings");
        warnings.addAll(jsonWarnings);
        warnings.addAll(context.failedUrls().isEmpty() ? List.of() : List.of("Some URLs failed to load and were skipped."));

        return new UniversitySourcesAnalysisResponse(
                sourceUrls,
                context.successfulUrls(),
                context.failedUrls(),
                readText(json, "summary", "Guidance generated from multiple university sources and your profile."),
                defaultIfEmpty(readStringList(json, "recommendedCareers"), seedCareers()),
                readStringList(json, "recommendedProgrammes"),
                readStringList(json, "recommendedUniversities"),
                readStringList(json, "keyRequirements"),
                readStringList(json, "skillGaps"),
                readStringList(json, "recommendedNextSteps"),
                dedupe(warnings),
                context.successfulUrls().size(),
                normalizeScore(json.path("suitabilityScore").asInt(65)),
                modelName
        );
    }

    private UniversitySourcesAnalysisResponse fallbackResponse(List<String> sourceUrls,
                                                               UniversitySourcesAggregatorService.AggregatedUniversityContext context,
                                                               List<String> warnings,
                                                               String modelName) {
        List<String> keyRequirements = new ArrayList<>();
        if (context.extractedKeywords().stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).contains("entry"))) {
            keyRequirements.add("Check each programme page for minimum subject and APS requirements.");
        }
        keyRequirements.add("Mathematics and English are commonly expected for many technology and business programmes.");

        warnings.addAll(context.failedUrls().isEmpty() ? List.of() : List.of("Some URLs failed to load and were skipped."));

        return new UniversitySourcesAnalysisResponse(
                sourceUrls,
                context.successfulUrls(),
                context.failedUrls(),
                "Fallback guidance generated from extracted university metadata and student profile.",
                seedCareers(),
                List.of("BSc Computer Science", "BCom Information Systems", "Diploma in Information Technology"),
                inferUniversities(context.successfulUrls()),
                keyRequirements,
                List.of("Strengthen practical project portfolio", "Build consistent problem-solving practice"),
                List.of("Open programme detail pages", "Compare your subjects with admission criteria", "Upload transcript and CV"),
                dedupe(warnings),
                context.successfulUrls().size(),
                60,
                modelName
        );
    }

    private List<String> seedCareers() {
        return List.of(
                "Software Developer",
                "Data Analyst",
                "Systems Analyst",
                "IT Support Specialist",
                "Business Analyst",
                "Web Developer",
                "Cybersecurity Analyst",
                "QA Tester",
                "Project Coordinator",
                "Education Technology Assistant"
        );
    }

    private List<String> inferUniversities(List<String> urls) {
        return urls.stream().map(url -> {
            if (url.contains("unisa")) return "UNISA";
            if (url.contains("uj.ac.za")) return "University of Johannesburg";
            if (url.contains("wits")) return "University of the Witwatersrand";
            if (url.contains("up.ac.za")) return "University of Pretoria";
            return "Trusted university source";
        }).distinct().toList();
    }

    private List<String> readStringList(JsonNode json, String field) {
        JsonNode node = json.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String text = item.asText("").trim();
            if (!text.isBlank()) {
                values.add(text);
            }
        });
        return values;
    }

    private String readText(JsonNode json, String field, String defaultValue) {
        String value = json.path(field).asText("").trim();
        return value.isBlank() ? defaultValue : value;
    }

    private List<String> defaultIfEmpty(List<String> values, List<String> fallback) {
        return values.isEmpty() ? fallback : values;
    }

    private List<String> dedupe(List<String> values) {
        return values.stream().filter(v -> v != null && !v.isBlank()).distinct().toList();
    }

    private int normalizeScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
