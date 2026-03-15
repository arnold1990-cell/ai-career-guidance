package com.edurite.ai.service;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final MediaType JSON = MediaType.get("application/json");
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.gson = new Gson();
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(25))
                .writeTimeout(Duration.ofSeconds(25))
                .callTimeout(Duration.ofSeconds(30))
                .build();
    }

    public CareerAdviceResponse getCareerAdvice(CareerAdviceRequest request) {
        ensureApiKey();
        String modelText = invokeGemini(buildPrompt(request));
        return parseCareerAdvice(modelText);
    }

    public UniversitySourcesAnalysisResponse getUniversitySourcesAdvice(
            UniversitySourcesAnalysisRequest request,
            com.edurite.student.entity.StudentProfile profile,
            List<String> sourceUrls,
            List<UniversitySourcePageResult> fetchedPages,
            String combinedContext
    ) {
        List<String> successUrls = fetchedPages.stream().filter(UniversitySourcePageResult::success)
                .map(UniversitySourcePageResult::sourceUrl).toList();
        List<String> failedUrls = fetchedPages.stream().filter(p -> !p.success())
                .map(UniversitySourcePageResult::sourceUrl).toList();

        if (apiKey == null || apiKey.isBlank()) {
            return fallbackUniversityResponse(request, sourceUrls, successUrls, failedUrls,
                    List.of("AI model is unavailable, fallback guidance was generated from source metadata."));
        }

        try {
            String prompt = buildUniversityPrompt(request, profile, fetchedPages, combinedContext);
            String modelText = invokeGemini(prompt);
            UniversitySourcesAnalysisResponse parsed = parseUniversityAdvice(modelText, sourceUrls, successUrls, failedUrls);
            return enrichWithWarnings(parsed, failedUrls);
        } catch (Exception ex) {
            log.warn("University sources analysis fell back after model error: {}", ex.getMessage());
            return fallbackUniversityResponse(request, sourceUrls, successUrls, failedUrls,
                    List.of("Model parsing failed, fallback guidance was generated.", "Reason: " + ex.getMessage()));
        }
    }

    private void ensureApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Fallback path used: Gemini API key is missing, returning explicit AI unavailable error.");
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Career AI is currently unavailable. Gemini API key is not configured.");
        }
    }

    private String invokeGemini(String prompt) {
        String endpointPath = GeminiModelResolver.buildGenerateContentPath(model);
        String resolvedModel = GeminiModelResolver.resolveModelName(model);
        String endpoint = GEMINI_BASE_URL + endpointPath + "?key=" + apiKey.trim();

        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        payload.add("contents", contents);

        Request httpRequest = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .build();

        log.info("Starting Gemini call: model={}, endpointPath={}", resolvedModel, endpointPath);

        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            log.info("Gemini HTTP response received: status={}", response.code());
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                HttpStatus status = response.code() == 401 || response.code() == 403
                        ? HttpStatus.BAD_GATEWAY
                        : HttpStatus.SERVICE_UNAVAILABLE;
                throw new AiServiceException(status,
                        "Gemini request failed with status " + response.code() + ". " + trim(errorBody));
            }

            if (response.body() == null) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "Gemini returned an empty response body.");
            }

            String geminiBody = response.body().string();
            return extractModelText(geminiBody);
        } catch (IOException ex) {
            log.error("Gemini call failed due to IO issue.", ex);
            throw new AiServiceException(HttpStatus.GATEWAY_TIMEOUT,
                    "Gemini request timed out or failed: " + ex.getMessage());
        }
    }

    private String buildPrompt(CareerAdviceRequest request) {
        return """
                You are a career guidance assistant.
                Return ONLY strict JSON with this exact schema:
                {
                  \"recommendedCareers\": [
                    {
                      \"name\": \"string\",
                      \"matchScore\": 0,
                      \"reason\": \"string\",
                      \"improvements\": [\"string\"]
                    }
                  ]
                }
                Rules:
                - Output valid JSON only (no markdown, no prose).
                - Recommend 3-5 careers.
                - matchScore must be integer from 0 to 100.
                - reason and improvements should be concise and actionable.

                Student profile:
                qualificationLevel: %s
                interests: %s
                skills: %s
                location: %s
                """.formatted(
                sanitizePromptValue(request.qualificationLevel()),
                sanitizePromptValue(request.interests()),
                sanitizePromptValue(request.skills()),
                sanitizePromptValue(request.location())
        );
    }

    private String buildUniversityPrompt(UniversitySourcesAnalysisRequest request,
                                         com.edurite.student.entity.StudentProfile profile,
                                         List<UniversitySourcePageResult> fetchedPages,
                                         String combinedContext) {
        String pageMetadata = fetchedPages.stream()
                .map(page -> "%s | %s | %s | keywords=%s".formatted(
                        page.sourceUrl(), page.success() ? "success" : "failed", page.pageType(), page.extractedKeywords()))
                .reduce("", (a, b) -> a + "\n" + b);

        return """
                You are EduRite's academic and career guidance assistant.
                Return ONLY valid JSON with this schema:
                {
                  \"summary\": \"string\",
                  \"recommendedCareers\": [\"string\"],
                  \"recommendedProgrammes\": [\"string\"],
                  \"recommendedUniversities\": [\"string\"],
                  \"keyRequirements\": [\"string\"],
                  \"skillGaps\": [\"string\"],
                  \"recommendedNextSteps\": [\"string\"],
                  \"warnings\": [\"string\"],
                  \"suitabilityScore\": 0
                }

                Rules:
                - Return student-friendly, practical guidance.
                - The evidence comes from multiple universities and only shortlisted relevant pages.
                - Recommend at least %d careers if enough evidence exists.
                - Recommend at least %d university programmes if enough evidence exists.
                - Recommend universities grounded in the retrieved page list.
                - Mention limitation warnings when sources are generic list pages.
                - Do not hallucinate programme-specific requirements that do not appear in the sources.
                - Keep suitabilityScore between 0 and 100.

                Student profile:
                firstName: %s
                lastName: %s
                qualificationLevel: %s
                interests: %s
                skills: %s
                experience: %s
                location: %s
                cvUploaded: %s
                transcriptUploaded: %s

                Request focus:
                targetProgram: %s
                careerInterest: %s
                qualificationLevel: %s

                Source metadata:
                %s

                Combined academic context (truncated):
                %s

                Optional internal seed careers:
                Software Developer, Data Analyst, Systems Analyst, IT Support Specialist, Business Analyst,
                QA Tester, Accountant, Economist, Electrical Engineer, Civil Engineer, Teacher.
                """.formatted(
                request.safeMaxRecommendations(),
                request.safeMaxRecommendations(),
                sanitizePromptValue(profile.getFirstName()),
                sanitizePromptValue(profile.getLastName()),
                sanitizePromptValue(profile.getQualificationLevel()),
                sanitizePromptValue(profile.getInterests()),
                sanitizePromptValue(profile.getSkills()),
                sanitizePromptValue(profile.getExperience()),
                sanitizePromptValue(profile.getLocation()),
                profile.getCvFileUrl() != null,
                profile.getTranscriptFileUrl() != null,
                sanitizePromptValue(request.targetProgram()),
                sanitizePromptValue(request.careerInterest()),
                sanitizePromptValue(request.qualificationLevel()),
                pageMetadata,
                sanitizePromptValue(combinedContext)
        );
    }

    private String extractModelText(String geminiBody) {
        try {
            JsonObject root = JsonParser.parseString(geminiBody).getAsJsonObject();
            JsonArray candidates = root.has("candidates") ? root.getAsJsonArray("candidates") : null;
            if (candidates == null || candidates.isEmpty()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "Gemini returned no candidates.");
            }

            JsonObject first = candidates.get(0).getAsJsonObject();
            JsonObject content = first.has("content") ? first.getAsJsonObject("content") : null;
            JsonArray parts = content != null && content.has("parts") ? content.getAsJsonArray("parts") : null;
            if (parts == null || parts.isEmpty()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "Gemini returned no text parts.");
            }

            JsonObject textPart = parts.get(0).getAsJsonObject();
            if (!textPart.has("text")) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "Gemini text part was missing.");
            }
            return stripCodeFences(textPart.get("text").getAsString());
        } catch (IllegalStateException ex) {
            log.error("Gemini payload parsing failed before extracting model text.", ex);
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "Gemini returned malformed JSON payload.");
        }
    }

    private CareerAdviceResponse parseCareerAdvice(String modelText) {
        try {
            CareerAdviceResponse response = objectMapper.readValue(modelText, CareerAdviceResponse.class);
            if (response.recommendedCareers() == null || response.recommendedCareers().isEmpty()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "Gemini returned no career recommendations.");
            }

            List<CareerAdviceResponse.RecommendedCareer> sanitized = response.recommendedCareers().stream()
                    .map(item -> new CareerAdviceResponse.RecommendedCareer(
                            item.name(),
                            normalizeScore(item.matchScore()),
                            item.reason(),
                            item.improvements() == null ? List.of() : item.improvements()
                    ))
                    .toList();
            log.info("Gemini JSON parsed successfully: recommendations={}", sanitized.size());
            return new CareerAdviceResponse(sanitized);
        } catch (JsonProcessingException ex) {
            log.warn("Gemini JSON parse failure: contentSnippet={}", trim(modelText));
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "Gemini returned non-JSON or invalid JSON output.");
        }
    }

    private UniversitySourcesAnalysisResponse parseUniversityAdvice(String modelText,
                                                                    List<String> sourceUrls,
                                                                    List<String> successUrls,
                                                                    List<String> failedUrls) throws JsonProcessingException {
        UniversityModelResponse parsed = objectMapper.readValue(modelText, UniversityModelResponse.class);
        return new UniversitySourcesAnalysisResponse(
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                sanitizePromptValue(parsed.summary),
                defaultList(parsed.recommendedCareers),
                defaultList(parsed.recommendedProgrammes),
                defaultList(parsed.recommendedUniversities),
                defaultList(parsed.keyRequirements),
                defaultList(parsed.skillGaps),
                defaultList(parsed.recommendedNextSteps),
                defaultList(parsed.warnings),
                normalizeScore(parsed.suitabilityScore),
                GeminiModelResolver.resolveModelName(model)
        );
    }

    private UniversitySourcesAnalysisResponse enrichWithWarnings(UniversitySourcesAnalysisResponse response,
                                                                 List<String> failedUrls) {
        Set<String> warnings = new LinkedHashSet<>(defaultList(response.warnings()));
        if (!failedUrls.isEmpty()) {
            warnings.add("Some sources failed to load and were skipped.");
        }
        return new UniversitySourcesAnalysisResponse(
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
                response.summary(),
                response.recommendedCareers(),
                response.recommendedProgrammes(),
                response.recommendedUniversities(),
                response.keyRequirements(),
                response.skillGaps(),
                response.recommendedNextSteps(),
                new ArrayList<>(warnings),
                response.suitabilityScore(),
                response.rawModelUsed()
        );
    }

    private UniversitySourcesAnalysisResponse fallbackUniversityResponse(
            UniversitySourcesAnalysisRequest request,
            List<String> sourceUrls,
            List<String> successUrls,
            List<String> failedUrls,
            List<String> warnings
    ) {
        int max = request.safeMaxRecommendations();
        return new UniversitySourcesAnalysisResponse(
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                "Based on the available university sources and your profile, here are practical options to explore next.",
                List.of("Software Developer", "Data Analyst", "IT Support Specialist", "Business Analyst", "Systems Analyst")
                        .stream().limit(max).toList(),
                List.of("BSc Computer Science", "BCom Information Systems", "Diploma in IT", "BSc Engineering")
                        .stream().limit(max).toList(),
                sourceUrls.stream().map(this::toUniversityName).distinct().toList(),
                List.of("Check subject requirements on programme-specific pages", "Mathematics is commonly required for computing and engineering pathways"),
                List.of("Build a practical portfolio", "Strengthen analytical and communication skills"),
                List.of("Open programme detail pages", "Compare your subjects with entry requirements", "Upload your transcript and CV"),
                warnings,
                70,
                GeminiModelResolver.resolveModelName(model)
        );
    }

    private String toUniversityName(String url) {
        String normalized = url == null ? "" : url.toLowerCase();
        if (normalized.contains("unisa")) {
            return "UNISA";
        }
        if (normalized.contains("uj")) {
            return "University of Johannesburg";
        }
        return "University Source";
    }

    private String sanitizePromptValue(String value) {
        if (value == null) {
            return "not provided";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "not provided" : trimmed;
    }

    private Integer normalizeScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String stripCodeFences(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```json", "").replaceFirst("^```", "").trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 250 ? value : value.substring(0, 250) + "...";
    }

    private List<String> defaultList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private static class UniversityModelResponse {
        public String summary;
        public List<String> recommendedCareers;
        public List<String> recommendedProgrammes;
        public List<String> recommendedUniversities;
        public List<String> keyRequirements;
        public List<String> skillGaps;
        public List<String> recommendedNextSteps;
        public List<String> warnings;
        public Integer suitabilityScore;
    }
}
