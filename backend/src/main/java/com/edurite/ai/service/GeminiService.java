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
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final int MAX_GEMINI_RETRIES = 2;
    private static final long[] RETRY_BACKOFF_MS = {300L, 800L};
    private static final MediaType JSON = MediaType.get("application/json");
    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Value("${gemini.api-key:}")
    private String configuredApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com}")
    private String baseUrl;

    public GeminiService(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.gson = new Gson();
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(20))
                .readTimeout(Duration.ofSeconds(45))
                .writeTimeout(Duration.ofSeconds(45))
                .callTimeout(Duration.ofSeconds(60))
                .build();
    }

    @PostConstruct
    void logStartupConfiguration() {
        String resolvedKey = resolveApiKey();
        String resolvedModel = resolveModel();
        String resolvedBaseUrl = resolveBaseUrl();
        logEnvironmentPresence("startup");

        if (resolvedKey.isBlank()) {
            log.warn("Gemini configuration warning: API key is missing. Set gemini.api-key, gemini.api.key, or GEMINI_API_KEY. model={}, baseUrl={}",
                    resolvedModel, resolvedBaseUrl);
            return;
        }

        log.info("Gemini configuration loaded: apiKeyPresent=true, keyMask={}, model={}, baseUrl={}",
                maskSecret(resolvedKey), resolvedModel, resolvedBaseUrl);
    }



    public GeminiHealthCheck checkHealth() {
        GeminiRequestConfig config = resolveRequestConfig(false);
        String resolvedApiKey = config.apiKey();
        String resolvedModel = config.model();
        String endpoint = config.modelInfoEndpoint();

        if (resolvedApiKey.isBlank()) {
            return new GeminiHealthCheck(false, false, resolvedModel, endpoint,
                    "Gemini API key is missing.");
        }

        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("x-goog-api-key", resolvedApiKey)
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            int statusCode = response.code();
            if (statusCode >= 200 && statusCode < 300) {
                return new GeminiHealthCheck(true, true, resolvedModel, endpoint, "Gemini endpoint reachable.");
            }
            String body = readSnippet(response.body());
            log.info("Gemini health check response: status={}, endpoint={}", statusCode, endpoint);
            return new GeminiHealthCheck(true, false, resolvedModel, endpoint,
                    "Gemini endpoint check failed with status " + statusCode + ": " + trim(body));
        } catch (Exception ex) {
            log.warn("Gemini health check failed: model={}, endpoint={}, message={}", resolvedModel, endpoint, ex.getMessage(), ex);
            return new GeminiHealthCheck(true, false, resolvedModel, endpoint,
                    "Gemini endpoint check exception: " + ex.getMessage());
        }
    }

    public CareerAdviceResponse getCareerAdvice(CareerAdviceRequest request) {
        ensureApiKey();
        String modelText = invokeGemini(buildPrompt(request), resolveRequestConfig(true));
        return parseCareerAdvice(modelText);
    }

    public UniversitySourcesAnalysisResponse getUniversitySourcesAdvice(
            UniversitySourcesAnalysisRequest request,
            com.edurite.student.entity.StudentProfile profile,
            List<String> sourceUrls,
            List<UniversitySourcePageResult> fetchedPages,
            String combinedContext
    ) {
        List<String> safeSourceUrls = sourceUrls == null ? List.of() : sourceUrls;
        List<UniversitySourcePageResult> safeFetchedPages = fetchedPages == null ? List.of() : fetchedPages;
        String safeCombinedContext = combinedContext == null ? "" : combinedContext.trim();

        List<String> successUrls = safeFetchedPages.stream().filter(UniversitySourcePageResult::success)
                .map(UniversitySourcePageResult::sourceUrl).toList();
        List<String> failedUrls = safeFetchedPages.stream().filter(page -> !page.success())
                .map(UniversitySourcePageResult::sourceUrl).toList();
        int sourceUrlCount = safeSourceUrls.size();
        int fetchedPageCount = safeFetchedPages.size();
        int contextLength = safeCombinedContext.length();

        List<String> sourceLimitations = new ArrayList<>();
        if (sourceUrlCount == 0) {
            sourceLimitations.add("No external university sources were available for this request; guidance was generated from profile context.");
        }
        if (fetchedPageCount == 0) {
            sourceLimitations.add("No university pages were fetched; guidance was generated from profile context.");
        }
        if (contextLength == 0) {
            sourceLimitations.add("No combined source context was available; guidance was generated from profile context.");
        }

        log.info("University guidance context: sourceUrls={}, fetchedPages={}, successfulPages={}, failedPages={}, combinedContextLength={}",
                sourceUrlCount, fetchedPageCount, successUrls.size(), failedUrls.size(), contextLength);

        GeminiRequestConfig config = resolveRequestConfig(true);
        if (config.apiKey().isBlank() || config.model().isBlank() || config.baseUrl().isBlank()) {
            log.warn("Fallback path used before Gemini attempt: apiKeyPresent={}, modelPresent={}, baseUrlPresent={}",
                    !config.apiKey().isBlank(), !config.model().isBlank(), !config.baseUrl().isBlank());
            return fallbackUniversityResponse(request, safeSourceUrls, successUrls, failedUrls,
                    List.of("Live AI guidance is temporarily unavailable. Suggestions were generated from trusted EduRite data."));
        }

        try {
            String prompt = buildUniversityPrompt(request, profile, safeFetchedPages, safeCombinedContext);
            String modelText = invokeGemini(prompt, config);
            UniversitySourcesAnalysisResponse parsed = parseUniversityAdvice(modelText, safeSourceUrls, successUrls, failedUrls, safeCombinedContext);
            log.info("Fallback decision: Gemini succeeded, fallbackUsed=false");
            return withRuntimeWarnings(enrichWithWarnings(parsed, failedUrls), sourceLimitations);
        } catch (Exception ex) {
            log.warn("Fallback decision: Gemini failed after live attempt, fallbackUsed=true, reason={}", ex.getMessage(), ex);
            return fallbackUniversityResponse(request, safeSourceUrls, successUrls, failedUrls,
                    List.of("Live AI guidance is temporarily unavailable. Suggestions were generated from trusted EduRite data."));
        }
    }

    private String analysisModeLabel(List<String> successUrls) {
        return successUrls.isEmpty() ? "AI Guidance (no external sources)" : "Live Gemini multi-source";
    }

    private String sourceTrustLabel(List<String> successUrls) {
        return successUrls.isEmpty() ? "AI-generated (no sources)" : "Verified from University Sources";
    }

    private String confidenceLevel(List<String> sourceUrls, List<String> successUrls) {
        if (successUrls.isEmpty()) {
            return "LOW";
        }
        if (sourceUrls.isEmpty()) {
            return "MEDIUM";
        }
        double ratio = (double) successUrls.size() / (double) sourceUrls.size();
        return ratio >= 0.6 ? "HIGH" : "MEDIUM";
    }

    private boolean sourceBackedAnalysis(List<String> successUrls, String combinedContext) {
        return !successUrls.isEmpty() && combinedContext != null && !combinedContext.isBlank();
    }

    private List<String> extractGroundedClaims(String combinedContext) {
        if (combinedContext == null || combinedContext.isBlank()) {
            return List.of();
        }
        return Arrays.stream(combinedContext.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> line.startsWith("Source URL:")
                        || line.startsWith("Title:")
                        || line.startsWith("Type:")
                        || line.startsWith("University:")
                        || line.toLowerCase().contains("admission")
                        || line.toLowerCase().contains("requirements")
                        || line.toLowerCase().contains("mathematics")
                        || line.toLowerCase().contains("english")
                        || line.toLowerCase().contains("qualification"))
                .limit(40)
                .toList();
    }

    private void ensureApiKey() {
        if (resolveApiKey().isBlank()) {
            log.warn("Fallback path used: Gemini API key is missing, returning explicit AI unavailable error.");
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Career AI is currently unavailable. Gemini API key is not configured.");
        }
    }

    private String invokeGemini(String prompt, GeminiRequestConfig config) {
        logEnvironmentPresence("invokeGemini");

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
                .url(config.generateEndpoint())
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", config.apiKey())
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .build();

        log.info("Starting Gemini call: model={}, endpointPath={}, endpoint={}, apiKeyMask={}",
                config.model(), config.endpointPath(), config.generateEndpoint(), maskSecret(config.apiKey()));

        for (int attempt = 0; attempt <= MAX_GEMINI_RETRIES; attempt++) {
            boolean retry = attempt < MAX_GEMINI_RETRIES;
            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                int statusCode = response.code();
                String bodySnippet = readSnippet(response.body());
                log.info("Gemini HTTP response received: status={}, model={}, attempt={}, retried={}",
                        statusCode, config.model(), attempt + 1, attempt > 0);

                if (response.isSuccessful()) {
                    if (bodySnippet.isBlank()) {
                        throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                                "Gemini returned an empty response body.");
                    }
                    return extractModelText(bodySnippet);
                }

                boolean retriableStatus = statusCode == 408 || statusCode == 429 || statusCode >= 500;
                log.warn("Gemini call failed: status={}, model={}, retried={}, snippet={}",
                        statusCode, config.model(), attempt > 0, trim(bodySnippet));

                if (retry && retriableStatus) {
                    sleepBeforeRetry(attempt);
                    continue;
                }

                HttpStatus status = statusCode == 401 || statusCode == 403 || statusCode == 400
                        ? HttpStatus.BAD_GATEWAY
                        : HttpStatus.SERVICE_UNAVAILABLE;
                throw new AiServiceException(status,
                        "Gemini request failed with status " + statusCode + ". " + trim(bodySnippet));
            } catch (IOException ex) {
                log.warn("Gemini call IO failure: model={}, attempt={}, retried={}, message={}",
                        config.model(), attempt + 1, attempt > 0, ex.getMessage(), ex);
                if (retry) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw new AiServiceException(HttpStatus.GATEWAY_TIMEOUT,
                        "Gemini request timed out or failed.");
            }
        }

        throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "Gemini request failed after retries.");
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
                - Output valid JSON only (no markdown, no prose, no code fences).
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
                  "recommendedCareers": [
                    {
                      "name": "string",
                      "reason": "string",
                      "requirements": ["string"],
                      "relatedProgrammes": ["string"],
                      "recommendationBasis": "SOURCE_VERIFIED | PROFILE_ONLY"
                    }
                  ],
                  "recommendedProgrammes": [
                    {
                      "name": "string",
                      "university": "string",
                      "admissionRequirements": ["string"],
                      "notes": "string",
                      "recommendationBasis": "SOURCE_VERIFIED | PROFILE_ONLY"
                    }
                  ],
                  "recommendedUniversities": ["string"],
                  "minimumRequirements": ["string"],
                  "keyRequirements": ["string"],
                  "skillGaps": ["string"],
                  "recommendedNextSteps": ["string"],
                  "warnings": ["string"],
                  "summary": "string",
                  "suitabilityScore": 0
                }

                Rules:
                - Return student-friendly, practical guidance.
                - Output valid JSON only (no markdown, no code fences, no prose outside JSON).
                - Keep section order exactly: recommendedCareers, recommendedProgrammes, recommendedUniversities, skillGaps, recommendedNextSteps, warnings, summary.
                - Recommend at least %d careers if enough evidence exists.
                - Recommend at least %d university programmes if enough evidence exists.
                - Each recommended career must include specific requirements and relatedProgrammes.
                - Each recommended programme must include admissionRequirements and notes.
                - Do not include application due dates or deadline fields anywhere.
                - minimumRequirements MUST always mention Grade 12 passes, English, and Mathematics for mathematics-related pathways.
                - NEVER invent APS scores. Only mention APS if an APS value appears explicitly in Combined academic context.
                - If APS is not explicitly present in source context, write exactly: "Verify APS requirements from the official university website."
                - NEVER invent due dates, subject minimums, or qualification rules not present in source context.
                - Ground programmes and universities in the retrieved source content.
                - If source metadata/context is empty, still provide recommendations using only profile and request focus.
                - Mention limitation warnings when sources are generic list pages.
                - For each recommended item, include recommendationBasis as either SOURCE_VERIFIED or PROFILE_ONLY.
                - Keep suitabilityScore between 0 and 100.
                - If model cannot provide clean JSON, still provide the seven sections as plain headings with bullet points.

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

                Grounded source claims (must be used when recommendationBasis is SOURCE_VERIFIED):
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
                sanitizePromptValue(pageMetadata),
                sanitizePromptValue(combinedContext),
                sanitizePromptValue(String.join("
", extractGroundedClaims(combinedContext)))
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

            List<String> textParts = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                JsonObject candidate = candidates.get(i).getAsJsonObject();
                JsonObject content = candidate.has("content") ? candidate.getAsJsonObject("content") : null;
                JsonArray parts = content != null && content.has("parts") ? content.getAsJsonArray("parts") : null;
                if (parts == null) {
                    continue;
                }
                for (int j = 0; j < parts.size(); j++) {
                    JsonObject part = parts.get(j).getAsJsonObject();
                    if (part.has("text")) {
                        String value = part.get("text").getAsString();
                        if (!value.isBlank()) {
                            textParts.add(value.trim());
                        }
                    }
                }
            }

            if (textParts.isEmpty()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "Gemini returned no text parts.");
            }

            return stripCodeFences(String.join("\n", textParts));
        } catch (IllegalStateException ex) {
            log.error("Gemini payload parsing failed before extracting model text.", ex);
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "Gemini returned malformed JSON payload.");
        }
    }

    private CareerAdviceResponse parseCareerAdvice(String modelText) {
        try {
            CareerAdviceResponse response = objectMapper.readValue(extractLikelyJson(modelText), CareerAdviceResponse.class);
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
            return fallbackCareerAdvice(modelText);
        }
    }

    private UniversitySourcesAnalysisResponse parseUniversityAdvice(String modelText,
                                                                    List<String> sourceUrls,
                                                                    List<String> successUrls,
                                                                    List<String> failedUrls,
                                                                    String combinedContext) throws JsonProcessingException {
        try {
            UniversityModelResponse parsed = objectMapper.readValue(extractLikelyJson(modelText), UniversityModelResponse.class);
            return buildUniversityResponse(parsed, sourceUrls, successUrls, failedUrls, combinedContext);
        } catch (JsonProcessingException ex) {
            log.warn("University guidance JSON parse failed, attempting section-based parsing: reason={}, contentSnippet={}",
                    ex.getOriginalMessage(), trim(modelText));
            UniversityModelResponse sectionParsed = parseSectionedUniversityAdvice(modelText);
            if (sectionParsed == null) {
                log.warn("University guidance section-based parsing failed: model output was unusable. snippet={}", trim(modelText));
                throw ex;
            }
            return buildUniversityResponse(sectionParsed, sourceUrls, successUrls, failedUrls, combinedContext);
        }
    }

    private UniversitySourcesAnalysisResponse buildUniversityResponse(UniversityModelResponse parsed,
                                                                      List<String> sourceUrls,
                                                                      List<String> successUrls,
                                                                      List<String> failedUrls,
                                                                      String combinedContext) {
        List<String> minimumRequirements = enforceMinimumRequirements(defaultList(parsed.minimumRequirements), parsed);
        List<String> keyRequirements = mergeKeyAndMinimumRequirements(defaultList(parsed.keyRequirements), minimumRequirements);
        return new UniversitySourcesAnalysisResponse(
                true,
                false,
                null,
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                analysisModeLabel(successUrls),
                sourceTrustLabel(successUrls),
                confidenceLevel(sourceUrls, successUrls),
                sourceBackedAnalysis(successUrls, combinedContext),
                sanitizePromptValue(parsed.summary),
                defaultCareerList(parsed.recommendedCareers),
                defaultProgrammeList(parsed.recommendedProgrammes),
                defaultList(parsed.recommendedUniversities),
                minimumRequirements,
                keyRequirements,
                defaultList(parsed.skillGaps),
                defaultList(parsed.recommendedNextSteps),
                defaultList(parsed.warnings),
                normalizeScore(parsed.suitabilityScore),
                resolveModel()
        );
    }

    private UniversityModelResponse parseSectionedUniversityAdvice(String modelText) {
        if (modelText == null || modelText.isBlank()) {
            return null;
        }

        List<String> headers = List.of(
                "Recommended careers",
                "Recommended programmes",
                "Recommended universities",
                "Skill gaps",
                "Recommended next steps",
                "Next steps",
                "Warnings",
                "Summary"
        );

        String normalized = modelText.replace("\r", "").trim();
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String currentHeader = null;

        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String header = headers.stream().filter(h -> h.equalsIgnoreCase(trimmed.replace(":", ""))).findFirst().orElse(null);
            if (header != null) {
                currentHeader = header;
                sections.putIfAbsent(header, new ArrayList<>());
                continue;
            }
            if (currentHeader != null) {
                sections.get(currentHeader).add(stripBullet(trimmed));
            }
        }

        if (sections.isEmpty()) {
            return null;
        }

        UniversityModelResponse response = new UniversityModelResponse();
        response.recommendedCareers = sections.getOrDefault("Recommended careers", List.of()).stream()
                .filter(v -> !v.isBlank())
                .map(v -> {
                    UniversityModelResponse.RecommendedCareerPayload payload = new UniversityModelResponse.RecommendedCareerPayload();
                    payload.name = v;
                    payload.reason = "Derived from section-based fallback parsing.";
                    payload.requirements = List.of("Verify subject requirements with the university");
                    payload.relatedProgrammes = List.of();
                    payload.recommendationBasis = "PROFILE_ONLY";
                    return payload;
                }).toList();
        response.recommendedProgrammes = sections.getOrDefault("Recommended programmes", List.of()).stream()
                .filter(v -> !v.isBlank())
                .map(v -> {
                    UniversityModelResponse.RecommendedProgrammePayload payload = new UniversityModelResponse.RecommendedProgrammePayload();
                    payload.name = v;
                    payload.university = "University Source";
                    payload.admissionRequirements = List.of("Not found in fetched sources");
                    payload.notes = "Verify exact programme requirements from official university programme pages.";
                    payload.recommendationBasis = "PROFILE_ONLY";
                    return payload;
                }).toList();
        response.recommendedUniversities = sections.getOrDefault("Recommended universities", List.of());
        response.skillGaps = sections.getOrDefault("Skill gaps", List.of());
        response.recommendedNextSteps = sections.containsKey("Recommended next steps")
                ? sections.get("Recommended next steps")
                : sections.getOrDefault("Next steps", List.of());
        response.warnings = sections.getOrDefault("Warnings", List.of());
        response.summary = String.join(" ", sections.getOrDefault("Summary", List.of()));
        response.minimumRequirements = List.of();
        response.keyRequirements = List.of();
        response.suitabilityScore = 60;
        return response;
    }


    private UniversitySourcesAnalysisResponse enrichWithWarnings(UniversitySourcesAnalysisResponse response,
                                                                 List<String> failedUrls) {
        Set<String> warnings = new LinkedHashSet<>(defaultList(response.warnings()));
        if (!failedUrls.isEmpty()) {
            warnings.add("Some sources failed to load and were skipped.");
        }
        return new UniversitySourcesAnalysisResponse(
                response.aiLive(),
                response.fallbackUsed(),
                response.warningMessage(),
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
                response.analysisModeLabel(),
                response.sourceTrustLabel(),
                response.confidenceLevel(),
                response.sourceBackedAnalysis(),
                response.summary(),
                response.recommendedCareers(),
                response.recommendedProgrammes(),
                response.recommendedUniversities(),
                response.minimumRequirements(),
                response.keyRequirements(),
                response.skillGaps(),
                response.recommendedNextSteps(),
                new ArrayList<>(warnings),
                response.suitabilityScore(),
                response.rawModelUsed()
        );
    }


    private UniversitySourcesAnalysisResponse withRuntimeWarnings(UniversitySourcesAnalysisResponse response,
                                                                  List<String> runtimeWarnings) {
        if (runtimeWarnings == null || runtimeWarnings.isEmpty()) {
            return response;
        }

        LinkedHashSet<String> warnings = new LinkedHashSet<>(defaultList(response.warnings()));
        warnings.addAll(runtimeWarnings);

        return new UniversitySourcesAnalysisResponse(
                true,
                false,
                runtimeWarnings.get(0),
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
                response.analysisModeLabel(),
                response.sourceTrustLabel(),
                response.confidenceLevel(),
                response.sourceBackedAnalysis(),
                response.summary(),
                response.recommendedCareers(),
                response.recommendedProgrammes(),
                response.recommendedUniversities(),
                response.minimumRequirements(),
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
                false,
                true,
                "Live AI guidance is temporarily unavailable. Suggestions were generated from trusted EduRite data.",
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                analysisModeLabel(successUrls),
                sourceTrustLabel(successUrls),
                confidenceLevel(sourceUrls, successUrls),
                sourceBackedAnalysis(successUrls, ""),
                "Based on the available university sources and your profile, here are practical options to explore next.",
                List.of(
                                new UniversitySourcesAnalysisResponse.RecommendedCareer(
                                        "Software Developer",
                                        "Strong fit for students interested in technology and problem solving.",
                                        List.of("Programming fundamentals", "Mathematics and logical reasoning"),
                                        List.of("BSc Computer Science", "Diploma in IT"),
                                        "PROFILE_ONLY"
                                ),
                                new UniversitySourcesAnalysisResponse.RecommendedCareer(
                                        "Data Analyst",
                                        "Good pathway for students who enjoy working with numbers and insights.",
                                        List.of("Statistics basics", "Spreadsheet and data literacy"),
                                        List.of("BCom Information Systems", "BSc Computer Science"),
                                        "PROFILE_ONLY"
                                ),
                                new UniversitySourcesAnalysisResponse.RecommendedCareer(
                                        "IT Support Specialist",
                                        "Suitable for students interested in practical technology support roles.",
                                        List.of("Basic networking knowledge", "Troubleshooting and communication skills"),
                                        List.of("Diploma in IT"),
                                        "PROFILE_ONLY"
                                ),
                                new UniversitySourcesAnalysisResponse.RecommendedCareer(
                                        "Business Analyst",
                                        "Recommended when combining business interest with digital systems.",
                                        List.of("Business process understanding", "Communication and documentation"),
                                        List.of("BCom Information Systems"),
                                        "PROFILE_ONLY"
                                ),
                                new UniversitySourcesAnalysisResponse.RecommendedCareer(
                                        "Systems Analyst",
                                        "Useful for students interested in improving how systems work.",
                                        List.of("Systems thinking", "Problem analysis"),
                                        List.of("BSc Computer Science", "BCom Information Systems"),
                                        "PROFILE_ONLY"
                                ))
                        .stream().limit(max).toList(),
                List.of(
                                new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                                        "BSc Computer Science",
                                        "University Source",
                                        List.of("Not found in fetched sources"),
                                        "Programme requirements should be verified on official faculty pages.",
                                        "PROFILE_ONLY"
                                ),
                                new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                                        "BCom Information Systems",
                                        "University Source",
                                        List.of("Not found in fetched sources"),
                                        "Admission criteria were not explicitly available in fetched content.",
                                        "PROFILE_ONLY"
                                ),
                                new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                                        "Diploma in IT",
                                        "University Source",
                                        List.of("Not found in fetched sources"),
                                        "Check programme-specific pages for exact subject and score minimums.",
                                        "PROFILE_ONLY"
                                ),
                                new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                                        "BSc Engineering",
                                        "University Source",
                                        List.of("Not found in fetched sources"),
                                        "Use official admissions pages to confirm current requirements.",
                                        "PROFILE_ONLY"
                                ))
                        .stream().limit(max).toList(),
                sourceUrls.stream().map(this::toUniversityName).distinct().toList(),
                defaultMinimumRequirements(),
                List.of("Check subject requirements on programme-specific pages", "Mathematics is commonly required for quantitative pathways", "English proficiency is required for most programmes"),
                List.of("Build a practical portfolio", "Strengthen analytical and communication skills"),
                List.of("Open programme detail pages", "Compare your subjects with entry requirements", "Upload your transcript and CV"),
                warnings,
                70,
                resolveModel()
        );
    }

    private List<String> enforceMinimumRequirements(List<String> provided,
                                                    UniversityModelResponse parsed) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(defaultMinimumRequirements());
        merged.addAll(provided);
        merged.addAll(defaultList(parsed.keyRequirements).stream()
                .filter(item -> item.toLowerCase().contains("grade 12")
                        || item.toLowerCase().contains("mathematics")
                        || item.toLowerCase().contains("english"))
                .toList());
        return new ArrayList<>(merged);
    }

    private List<String> mergeKeyAndMinimumRequirements(List<String> keyRequirements,
                                                         List<String> minimumRequirements) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(minimumRequirements);
        merged.addAll(keyRequirements);
        return new ArrayList<>(merged);
    }

    private List<String> defaultMinimumRequirements() {
        return List.of(
                "Grade 12 passes are required for university admission pathways.",
                "Mathematics is required for mathematics-related programmes.",
                "English is required for admission and academic communication."
        );
    }

    private String stripBullet(String value) {
        return value.replaceFirst("^[-*•]+\s*", "").trim();
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

    private String sanitizeSourceBoundValue(String value) {
        if (value == null) {
            return "Not found in fetched sources";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "Not found in fetched sources" : trimmed;
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

    private String extractLikelyJson(String value) {
        String cleaned = stripCodeFences(value);
        if (cleaned == null || cleaned.isBlank()) {
            return "";
        }

        int objectStart = cleaned.indexOf('{');
        int objectEnd = cleaned.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return cleaned.substring(objectStart, objectEnd + 1).trim();
        }

        int arrayStart = cleaned.indexOf('[');
        int arrayEnd = cleaned.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return cleaned.substring(arrayStart, arrayEnd + 1).trim();
        }
        return cleaned.trim();
    }

    private CareerAdviceResponse fallbackCareerAdvice(String modelText) {
        List<String> lines = modelText == null ? List.of() : modelText.lines()
                .map(this::stripBullet)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.toLowerCase().startsWith("recommended careers"))
                .distinct()
                .limit(5)
                .toList();

        List<CareerAdviceResponse.RecommendedCareer> careers = lines.stream()
                .map(line -> new CareerAdviceResponse.RecommendedCareer(
                        line.length() > 70 ? line.substring(0, 70) : line,
                        65,
                        "Based on your profile and current guidance context.",
                        List.of("Review this option with your school counsellor.")
                ))
                .toList();

        if (careers.isEmpty()) {
            careers = List.of(
                    new CareerAdviceResponse.RecommendedCareer(
                            "General career exploration",
                            60,
                            "Broaden your choices using your interests and strengths.",
                            List.of("Update your profile and try AI guidance again.")
                    )
            );
        }

        return new CareerAdviceResponse(careers.stream()
                .sorted(Comparator.comparing(CareerAdviceResponse.RecommendedCareer::name))
                .toList());
    }

    private String readSnippet(ResponseBody responseBody) throws IOException {
        return responseBody == null ? "" : responseBody.string();
    }

    private String resolveApiKey() {
        String fromConfig = configuredApiKey == null ? "" : configuredApiKey.trim();
        if (!fromConfig.isBlank()) {
            return fromConfig;
        }
        String dotNotation = environment.getProperty("gemini.api.key", "").trim();
        if (!dotNotation.isBlank()) {
            return dotNotation;
        }
        String kebabNotation = environment.getProperty("gemini.api-key", "").trim();
        if (!kebabNotation.isBlank()) {
            return kebabNotation;
        }
        return environment.getProperty("GEMINI_API_KEY", "").trim();
    }

    private String resolveConfiguredModelInput() {
        String configured = model == null ? "" : model.trim();
        if (!configured.isBlank()) {
            return configured;
        }

        String fromProperty = environment.getProperty("gemini.model", "").trim();
        if (!fromProperty.isBlank()) {
            return fromProperty;
        }

        return environment.getProperty("GEMINI_MODEL", "").trim();
    }

    private String resolveModel() {
        return GeminiModelResolver.resolveModelName(resolveConfiguredModelInput());
    }

    private String resolveBaseUrl() {
        String resolved = baseUrl == null ? "" : baseUrl.trim();
        if (resolved.isBlank()) {
            resolved = environment.getProperty("gemini.base-url", "").trim();
        }
        if (resolved.isBlank()) {
            resolved = environment.getProperty("gemini.base.url", "").trim();
        }
        if (resolved.isBlank()) {
            resolved = environment.getProperty("GEMINI_BASE_URL", "").trim();
        }
        if (resolved.isBlank()) {
            resolved = GEMINI_BASE_URL;
        }
        String normalized = GeminiModelResolver.normalizeBaseUrl(resolved);
        return normalized.isBlank() ? GEMINI_BASE_URL : normalized;
    }

    private GeminiRequestConfig resolveRequestConfig(boolean logValues) {
        String configuredModelInput = resolveConfiguredModelInput();
        String resolvedModel = GeminiModelResolver.resolveModelName(configuredModelInput);
        String resolvedBaseUrl = resolveBaseUrl();
        String endpointPath = GeminiModelResolver.buildGenerateContentPath(configuredModelInput, resolvedBaseUrl);
        String modelInfoPath = GeminiModelResolver.buildModelInfoPath(configuredModelInput, resolvedBaseUrl);
        String resolvedApiKey = resolveApiKey();
        GeminiRequestConfig config = new GeminiRequestConfig(
                resolvedApiKey,
                resolvedModel,
                resolvedBaseUrl,
                endpointPath,
                resolvedBaseUrl + endpointPath,
                resolvedBaseUrl + modelInfoPath
        );
        if (logValues) {
            log.info("Gemini request config: apiKeyPresent={}, keyMask={}, model={}, baseUrl={}, endpoint={}",
                    !resolvedApiKey.isBlank(),
                    maskSecret(resolvedApiKey),
                    resolvedModel,
                    resolvedBaseUrl,
                    config.generateEndpoint());
        }
        return config;
    }

    private void logEnvironmentPresence(String context) {
        boolean envApiKeyPresent = !environment.getProperty("GEMINI_API_KEY", "").isBlank();
        boolean envModelPresent = !environment.getProperty("GEMINI_MODEL", "").isBlank();
        boolean envBaseUrlPresent = !environment.getProperty("GEMINI_BASE_URL", "").isBlank();
        boolean propertyApiKeyPresent = !environment.getProperty("gemini.api-key", "").isBlank()
                || !environment.getProperty("gemini.api.key", "").isBlank();
        boolean propertyModelPresent = !environment.getProperty("gemini.model", "").isBlank();
        boolean propertyBaseUrlPresent = !environment.getProperty("gemini.base-url", "").isBlank()
                || !environment.getProperty("gemini.base.url", "").isBlank();

        log.info("Gemini config diagnostics [{}]: envApiKeyPresent={}, envModelPresent={}, envBaseUrlPresent={}, propertyApiKeyPresent={}, propertyModelPresent={}, propertyBaseUrlPresent={}, resolvedModel={}, resolvedBaseUrl={}",
                context,
                envApiKeyPresent,
                envModelPresent,
                envBaseUrlPresent,
                propertyApiKeyPresent,
                propertyModelPresent,
                propertyBaseUrlPresent,
                resolveModel(),
                resolveBaseUrl());
    }

    private void sleepBeforeRetry(int attempt) {
        long backoff = RETRY_BACKOFF_MS[Math.min(attempt, RETRY_BACKOFF_MS.length - 1)];
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "<empty>";
        }
        String trimmed = secret.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "***" + trimmed.substring(trimmed.length() - 4);
    }

    private List<String> defaultList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private List<String> defaultListOrNotFound(List<String> value) {
        if (value == null || value.isEmpty()) {
            return List.of("Not found in fetched sources");
        }
        return value;
    }

    private String normalizeRecommendationBasis(String value) {
        if (value == null || value.isBlank()) {
            return "PROFILE_ONLY";
        }
        String normalized = value.trim().toUpperCase();
        return normalized.equals("SOURCE_VERIFIED") ? "SOURCE_VERIFIED" : "PROFILE_ONLY";
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedCareer> defaultCareerList(
            List<UniversityModelResponse.RecommendedCareerPayload> value) {
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .filter(item -> item != null && item.name != null && !item.name.isBlank())
                .map(item -> new UniversitySourcesAnalysisResponse.RecommendedCareer(
                        item.name,
                        sanitizeSourceBoundValue(item.reason),
                        defaultListOrNotFound(item.requirements),
                        defaultList(item.relatedProgrammes),
                        normalizeRecommendationBasis(item.recommendationBasis)
                ))
                .toList();
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedProgramme> defaultProgrammeList(
            List<UniversityModelResponse.RecommendedProgrammePayload> value) {
        if (value == null) {
            return List.of();
        }
        return value.stream()
                .filter(item -> item != null && item.name != null && !item.name.isBlank())
                .map(item -> new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                        item.name,
                        sanitizeSourceBoundValue(item.university),
                        defaultListOrNotFound(item.admissionRequirements),
                        sanitizeSourceBoundValue(item.notes),
                        normalizeRecommendationBasis(item.recommendationBasis)
                ))
                .toList();
    }



    public record GeminiHealthCheck(
            boolean apiKeyPresent,
            boolean endpointReachable,
            String model,
            String endpoint,
            String message
    ) {
    }

    private static class UniversityModelResponse {
        public String summary;
        public List<RecommendedCareerPayload> recommendedCareers;
        public List<RecommendedProgrammePayload> recommendedProgrammes;
        public List<String> recommendedUniversities;
        public List<String> minimumRequirements;
        public List<String> keyRequirements;
        public List<String> skillGaps;
        public List<String> recommendedNextSteps;
        public List<String> warnings;
        public Integer suitabilityScore;

        private static class RecommendedCareerPayload {
            public String name;
            public String reason;
            public List<String> requirements;
            public List<String> relatedProgrammes;
            public String recommendationBasis;
        }

        private static class RecommendedProgrammePayload {
            public String name;
            public String university;
            public List<String> admissionRequirements;
            public String notes;
            public String recommendationBasis;
        }
    }

    private record GeminiRequestConfig(
            String apiKey,
            String model,
            String baseUrl,
            String endpointPath,
            String generateEndpoint,
            String modelInfoEndpoint
    ) {
    }
}
