package com.edurite.ai.service;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.university.UniversityCrawlFailureType;
import com.edurite.ai.university.UniversityPageType;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final MediaType JSON = MediaType.get("application/json");
    private static final int MAX_GEMINI_RETRIES = 3;
    private static final long[] RETRY_BACKOFF_MS = {500L, 1_000L, 2_000L};
    private static final int RAW_RESPONSE_LOG_LIMIT = 2_000;
    private static final int MAX_MODEL_TEXT_CHARS = 24_000;

    // Keep Gemini configuration hardcoded in Java as requested.
    private static final String API_KEY = "PASTE_GEMINI_API_KEY_HERE";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String MODEL = "gemini-2.5-flash";

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final ObjectMapper objectMapper;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.gson = new Gson();
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(20))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .callTimeout(Duration.ofSeconds(75))
                .build();
    }

    public GeminiService(ObjectMapper objectMapper, Object ignoredConfigurationSource) {
        this(objectMapper);
    }

    public GeminiHealthCheck checkHealth() {
        GeminiRequestConfig config = requestConfig();
        if (config.apiKey().isBlank() || config.apiKey().contains("PASTE_GEMINI_API_KEY_HERE")) {
            return new GeminiHealthCheck(false, false, config.model(), config.modelInfoEndpoint(),
                    "Gemini API key is still a placeholder in GeminiService.");
        }

        Request request = new Request.Builder()
                .url(config.modelInfoEndpoint())
                .addHeader("x-goog-api-key", config.apiKey())
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return new GeminiHealthCheck(true, true, config.model(), config.modelInfoEndpoint(), "Gemini endpoint reachable.");
            }
            return new GeminiHealthCheck(true, false, config.model(), config.modelInfoEndpoint(),
                    "Gemini endpoint returned HTTP " + response.code());
        } catch (IOException ex) {
            log.warn("Gemini health check failed: model={}, endpoint={}", config.model(), config.modelInfoEndpoint(), ex);
            return new GeminiHealthCheck(true, false, config.model(), config.modelInfoEndpoint(), ex.getMessage());
        }
    }

    public CareerAdviceResponse getCareerAdvice(CareerAdviceRequest request) {
        ensureGeminiConfigured();
        String modelText = invokeGemini(buildCareerPrompt(request), RequestMode.CAREER_ADVICE, List.of(), List.of(), "");
        return parseCareerAdvice(modelText);
    }

    public UniversitySourcesAnalysisResponse getUniversitySourcesAdvice(
            UniversitySourcesAnalysisRequest request,
            StudentProfile profile,
            List<String> sourceUrls,
            List<UniversitySourcePageResult> fetchedPages,
            String combinedContext
    ) {
        List<String> safeSourceUrls = sourceUrls == null ? List.of() : sourceUrls;
        List<UniversitySourcePageResult> safeFetchedPages = fetchedPages == null ? List.of() : fetchedPages;
        String safeCombinedContext = combinedContext == null ? "" : combinedContext.trim();
        List<String> successUrls = safeFetchedPages.stream().filter(UniversitySourcePageResult::success).map(UniversitySourcePageResult::sourceUrl).toList();
        List<String> failedUrls = safeFetchedPages.stream().filter(page -> !page.success()).map(UniversitySourcePageResult::sourceUrl).toList();

        log.info("University guidance request: model={}, endpoint={}, requestMode={}, requestedUrlCount={}, fetchedUrlCount={}, successfulUrlCount={}, failedUrlCount={}, combinedContextLength={}",
                MODEL,
                requestConfig().generateEndpoint(),
                RequestMode.UNIVERSITY_GUIDANCE,
                safeSourceUrls.size(),
                safeFetchedPages.size(),
                successUrls.size(),
                failedUrls.size(),
                safeCombinedContext.length());

        List<String> runtimeWarnings = buildRuntimeWarnings(safeFetchedPages, safeCombinedContext, successUrls, failedUrls);
        if (safeCombinedContext.isBlank()) {
            return fallbackUniversityResponse(request, safeSourceUrls, successUrls, failedUrls,
                    appendWarning(runtimeWarnings, "Fallback used because no public university page body text was available for analysis."));
        }

        try {
            ensureGeminiConfigured();
            String prompt = buildUniversityPrompt(request, profile, safeFetchedPages, safeCombinedContext);
            String modelText = invokeGemini(prompt, RequestMode.UNIVERSITY_GUIDANCE, safeSourceUrls, safeFetchedPages, safeCombinedContext);
            UniversitySourcesAnalysisResponse parsed = parseUniversityAdvice(modelText, safeSourceUrls, successUrls, failedUrls);
            return mergeWarnings(parsed, runtimeWarnings);
        } catch (Exception ex) {
            log.error("Gemini university guidance failed. Falling back. requestedUrlCount={}, fetchedUrlCount={}, combinedContextLength={}",
                    safeSourceUrls.size(), safeFetchedPages.size(), safeCombinedContext.length(), ex);
            return fallbackUniversityResponse(request, safeSourceUrls, successUrls, failedUrls,
                    appendWarning(runtimeWarnings, "Fallback used because Gemini live guidance failed: " + safeMessage(ex)));
        }
    }

    private void ensureGeminiConfigured() {
        if (API_KEY.isBlank() || API_KEY.contains("PASTE_GEMINI_API_KEY_HERE")) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API key is not configured in GeminiService. Replace the hardcoded placeholder API_KEY constant.");
        }
    }

    private String invokeGemini(String prompt,
                                RequestMode requestMode,
                                List<String> sourceUrls,
                                List<UniversitySourcePageResult> fetchedPages,
                                String combinedContext) {
        GeminiRequestConfig config = requestConfig();
        String payload = buildGeminiPayload(prompt);
        Request request = new Request.Builder()
                .url(config.generateEndpoint())
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", config.apiKey())
                .post(RequestBody.create(payload, JSON))
                .build();

        for (int attempt = 1; attempt <= MAX_GEMINI_RETRIES; attempt++) {
            try (Response response = okHttpClient.newCall(request).execute()) {
                String rawBody = readResponseBody(response.body());
                log.info("Gemini call completed: model={}, endpoint={}, requestMode={}, attempt={}, requestedUrlCount={}, fetchedUrlCount={}, combinedContextLength={}, httpStatus={}",
                        config.model(), config.generateEndpoint(), requestMode, attempt, sourceUrls.size(), fetchedPages.size(), combinedContext.length(), response.code());
                log.info("Gemini raw response (truncated): {}", truncate(rawBody, RAW_RESPONSE_LOG_LIMIT));

                if (response.isSuccessful()) {
                    return extractModelText(rawBody);
                }

                if (shouldRetry(response.code()) && attempt < MAX_GEMINI_RETRIES) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw new IOException("Gemini HTTP " + response.code() + ": " + truncate(rawBody, 500));
            } catch (IOException ex) {
                log.error("Gemini call error: model={}, endpoint={}, requestMode={}, attempt={}",
                        config.model(), config.generateEndpoint(), requestMode, attempt, ex);
                if (attempt >= MAX_GEMINI_RETRIES) {
                    throw new RuntimeException("Gemini request failed after retries", ex);
                }
                sleepBeforeRetry(attempt);
            }
        }
        throw new RuntimeException("Gemini request exhausted retries without a result");
    }

    private String buildGeminiPayload(String prompt) {
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
        return gson.toJson(payload);
    }

    private boolean shouldRetry(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private String extractModelText(String rawBody) throws IOException {
        JsonNode root = objectMapper.readTree(rawBody);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IOException("Gemini response did not contain candidates");
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode candidate : candidates) {
            for (JsonNode part : candidate.path("content").path("parts")) {
                String text = part.path("text").asText("");
                if (!text.isBlank()) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(text.trim());
                }
            }
        }
        String result = builder.toString().trim();
        if (result.isBlank()) {
            throw new IOException("Gemini response did not contain text parts");
        }
        return truncate(result, MAX_MODEL_TEXT_CHARS);
    }

    private CareerAdviceResponse parseCareerAdvice(String modelText) {
        List<String> lines = modelText == null ? List.of() : modelText.lines()
                .map(this::stripBullet)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .distinct()
                .limit(5)
                .toList();

        List<CareerAdviceResponse.RecommendedCareer> careers = new ArrayList<>();
        for (String line : lines) {
            careers.add(new CareerAdviceResponse.RecommendedCareer(line, 65,
                    "Based on the live Gemini career guidance output.",
                    List.of("Verify the fit with your school subjects and goals.")));
        }
        if (careers.isEmpty()) {
            careers.add(new CareerAdviceResponse.RecommendedCareer("Career exploration", 50,
                    "Gemini returned limited output.",
                    List.of("Try again with more profile detail.")));
        }
        return new CareerAdviceResponse(careers);
    }

    private UniversitySourcesAnalysisResponse parseUniversityAdvice(String modelText,
                                                                    List<String> sourceUrls,
                                                                    List<String> successUrls,
                                                                    List<String> failedUrls) {
        String recoveredJson = recoverJson(modelText);
        if (recoveredJson.isBlank()) {
            throw new RuntimeException("Gemini returned no recoverable JSON payload");
        }

        try {
            UniversityModelResponse parsed = objectMapper.readValue(recoveredJson, UniversityModelResponse.class);
            return buildUniversityResponse(parsed, sourceUrls, successUrls, failedUrls);
        } catch (JsonProcessingException ex) {
            log.warn("Gemini JSON parse failed, attempting structured recovery. payload={}", truncate(recoveredJson, 1000), ex);
            UniversityModelResponse recovered = recoverStructuredResponse(recoveredJson);
            if (recovered != null) {
                return buildUniversityResponse(recovered, sourceUrls, successUrls, failedUrls);
            }
            throw new RuntimeException("Gemini returned malformed JSON that could not be recovered", ex);
        }
    }

    private UniversityModelResponse recoverStructuredResponse(String recoveredJson) {
        try {
            JsonNode root = objectMapper.readTree(recoveredJson);
            if (!root.isObject()) {
                return null;
            }
            UniversityModelResponse response = new UniversityModelResponse();
            response.summary = textOrDefault(root, "summary", "Not found in analysed page content");
            response.recommendedUniversities = readStringList(root, "recommendedUniversities");
            response.minimumRequirements = readStringList(root, "minimumRequirements");
            response.keyRequirements = readStringList(root, "keyRequirements");
            response.skillGaps = readStringList(root, "skillGaps");
            response.recommendedNextSteps = readStringList(root, "recommendedNextSteps");
            response.warnings = readStringList(root, "warnings");
            response.suitabilityScore = root.path("suitabilityScore").isNumber() ? root.path("suitabilityScore").asInt() : 0;
            response.recommendedCareers = new ArrayList<>();
            for (JsonNode node : root.path("recommendedCareers")) {
                RecommendedCareerPayload payload = new RecommendedCareerPayload();
                payload.name = textOrDefault(node, "name", "Career option");
                payload.reason = textOrDefault(node, "reason", "Not found in analysed page content");
                payload.requirements = readStringList(node, "requirements");
                payload.relatedProgrammes = readStringList(node, "relatedProgrammes");
                response.recommendedCareers.add(payload);
            }
            response.recommendedProgrammes = new ArrayList<>();
            for (JsonNode node : root.path("recommendedProgrammes")) {
                RecommendedProgrammePayload payload = new RecommendedProgrammePayload();
                payload.name = textOrDefault(node, "name", "Programme option");
                payload.university = textOrDefault(node, "university", "Verify on the official university programme page");
                payload.admissionRequirements = readStringList(node, "admissionRequirements");
                payload.notes = textOrDefault(node, "notes", "Not found in analysed page content");
                response.recommendedProgrammes.add(payload);
            }
            return response;
        } catch (Exception ex) {
            log.warn("Structured Gemini recovery failed", ex);
            return null;
        }
    }

    private UniversitySourcesAnalysisResponse buildUniversityResponse(UniversityModelResponse parsed,
                                                                      List<String> sourceUrls,
                                                                      List<String> successUrls,
                                                                      List<String> failedUrls) {
        List<String> minimumRequirements = ensureEvidenceText(parsed.minimumRequirements);
        List<String> keyRequirements = ensureEvidenceText(parsed.keyRequirements);
        List<String> skillGaps = ensureEvidenceText(parsed.skillGaps);
        List<String> nextSteps = ensureNextSteps(parsed.recommendedNextSteps);
        List<String> warnings = ensureWarnings(parsed.warnings, failedUrls, successUrls);

        return new UniversitySourcesAnalysisResponse(
                true,
                false,
                warnings.isEmpty() ? null : warnings.get(0),
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                safeText(parsed.summary, "Not found in analysed page content"),
                toRecommendedCareers(parsed.recommendedCareers),
                toRecommendedProgrammes(parsed.recommendedProgrammes),
                ensureEvidenceText(parsed.recommendedUniversities),
                minimumRequirements,
                keyRequirements,
                skillGaps,
                nextSteps,
                warnings,
                normalizeScore(parsed.suitabilityScore),
                MODEL
        );
    }

    private UniversitySourcesAnalysisResponse fallbackUniversityResponse(UniversitySourcesAnalysisRequest request,
                                                                         List<String> sourceUrls,
                                                                         List<String> successUrls,
                                                                         List<String> failedUrls,
                                                                         List<String> warnings) {
        List<String> combinedWarnings = new ArrayList<>(warnings);
        if (combinedWarnings.stream().noneMatch(item -> item.toLowerCase(Locale.ROOT).contains("fallback"))) {
            combinedWarnings.add(0, "Fallback recommendations are shown because live Gemini multi-source guidance was unavailable.");
        }

        return new UniversitySourcesAnalysisResponse(
                false,
                true,
                combinedWarnings.isEmpty() ? "Fallback recommendations were used." : combinedWarnings.get(0),
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                "Fallback recommendations only. These suggestions are not a live evidence-based university analysis. Verify every programme detail on the official university programme page.",
                List.of(new UniversitySourcesAnalysisResponse.RecommendedCareer(
                        safeText(request.careerInterest(), "Career of interest"),
                        "Fallback recommendation only. Not based on live Gemini output.",
                        List.of("Not found in analysed page content", "Verify on the official university programme page"),
                        List.of(safeText(request.targetProgram(), "Programme of interest"))
                )),
                List.of(new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                        safeText(request.targetProgram(), "Programme of interest"),
                        "Verify on the official university programme page",
                        List.of("Not found in analysed page content", "Verify on the official university programme page"),
                        "Fallback recommendation only. Official programme details were not confirmed live."
                )),
                List.of("Verify on the official university programme page"),
                List.of("Not found in analysed page content", "Verify on the official university programme page"),
                List.of("Not found in analysed page content", "Verify on the official university programme page"),
                List.of("Not found in analysed page content", "Verify on the official university programme page"),
                List.of("Retry live guidance later", "Open official university admissions and programme pages", "Verify on the official university programme page"),
                combinedWarnings,
                0,
                MODEL
        );
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedCareer> toRecommendedCareers(List<RecommendedCareerPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return List.of();
        }
        List<UniversitySourcesAnalysisResponse.RecommendedCareer> results = new ArrayList<>();
        for (RecommendedCareerPayload payload : payloads) {
            if (payload == null || safeText(payload.name, "").isBlank()) {
                continue;
            }
            results.add(new UniversitySourcesAnalysisResponse.RecommendedCareer(
                    payload.name.trim(),
                    safeText(payload.reason, "Not found in analysed page content"),
                    ensureEvidenceText(payload.requirements),
                    ensureEvidenceText(payload.relatedProgrammes)
            ));
        }
        return results;
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedProgramme> toRecommendedProgrammes(List<RecommendedProgrammePayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return List.of();
        }
        List<UniversitySourcesAnalysisResponse.RecommendedProgramme> results = new ArrayList<>();
        for (RecommendedProgrammePayload payload : payloads) {
            if (payload == null || safeText(payload.name, "").isBlank()) {
                continue;
            }
            results.add(new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                    payload.name.trim(),
                    safeText(payload.university, "Verify on the official university programme page"),
                    ensureEvidenceText(payload.admissionRequirements),
                    safeText(payload.notes, "Not found in analysed page content")
            ));
        }
        return results;
    }

    private List<String> ensureEvidenceText(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of("Not found in analysed page content", "Verify on the official university programme page");
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String value : values) {
            String item = safeText(value, "");
            if (!item.isBlank()) {
                cleaned.add(item);
            }
        }
        if (cleaned.isEmpty()) {
            return List.of("Not found in analysed page content", "Verify on the official university programme page");
        }
        return List.copyOf(cleaned);
    }

    private List<String> ensureNextSteps(List<String> values) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(ensureEvidenceText(values));
        merged.add("Verify on the official university programme page");
        return List.copyOf(merged);
    }

    private List<String> ensureWarnings(List<String> values, List<String> failedUrls, List<String> successUrls) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String cleaned = safeText(value, "");
                if (!cleaned.isBlank()) {
                    warnings.add(cleaned);
                }
            }
        }
        if (!failedUrls.isEmpty()) {
            warnings.add("Some public university pages failed to load or extract: " + failedUrls.size() + " page(s). ");
        }
        if (successUrls.size() <= 1) {
            warnings.add("Evidence is limited because only a small amount of public university content was analysed.");
        }
        return List.copyOf(warnings);
    }

    private UniversitySourcesAnalysisResponse mergeWarnings(UniversitySourcesAnalysisResponse response, List<String> runtimeWarnings) {
        LinkedHashSet<String> mergedWarnings = new LinkedHashSet<>();
        if (response.warnings() != null) {
            mergedWarnings.addAll(response.warnings());
        }
        mergedWarnings.addAll(runtimeWarnings);
        List<String> warnings = List.copyOf(mergedWarnings);
        return new UniversitySourcesAnalysisResponse(
                response.aiLive(),
                response.fallbackUsed(),
                response.warningMessage() == null && !warnings.isEmpty() ? warnings.get(0) : response.warningMessage(),
                response.sourceUrls(),
                response.successfullyAnalysedUrls(),
                response.failedUrls(),
                response.totalSourcesUsed(),
                response.summary(),
                response.recommendedCareers(),
                response.recommendedProgrammes(),
                response.recommendedUniversities(),
                response.minimumRequirements(),
                response.keyRequirements(),
                response.skillGaps(),
                response.recommendedNextSteps(),
                warnings,
                response.suitabilityScore(),
                response.rawModelUsed()
        );
    }

    private List<String> buildRuntimeWarnings(List<UniversitySourcePageResult> fetchedPages,
                                              String combinedContext,
                                              List<String> successUrls,
                                              List<String> failedUrls) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (successUrls.isEmpty()) {
            warnings.add("No public university sources were analysed successfully.");
        }
        if (!failedUrls.isEmpty()) {
            warnings.add("Failed pages: " + failedUrls.size() + ". Some official pages could not be fetched or extracted.");
        }
        if (combinedContext.isBlank()) {
            warnings.add("Limited public content: no visible page body text was available after extraction.");
        } else if (combinedContext.length() < 1_500) {
            warnings.add("Weak evidence: only a small amount of visible page text was available for analysis.");
        }
        for (UniversitySourcePageResult page : fetchedPages) {
            if (!page.success() && page.failureReason() != null && !page.failureReason().isBlank()) {
                warnings.add("Failed page " + page.sourceUrl() + ": " + page.failureReason());
            }
        }
        return List.copyOf(warnings);
    }

    private List<String> appendWarning(List<String> warnings, String extra) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(warnings);
        merged.add(extra);
        return List.copyOf(merged);
    }

    private String buildCareerPrompt(CareerAdviceRequest request) {
        return "You are an education guidance assistant. Recommend careers based only on the student profile below.\n"
                + "Return a short bullet list.\n\n"
                + "Qualification level: " + safeText(request.qualificationLevel(), "not provided") + "\n"
                + "Interests: " + safeText(request.interests(), "not provided") + "\n"
                + "Skills: " + safeText(request.skills(), "not provided") + "\n"
                + "Location: " + safeText(request.location(), "not provided");
    }

    private String buildUniversityPrompt(UniversitySourcesAnalysisRequest request,
                                         StudentProfile profile,
                                         List<UniversitySourcePageResult> fetchedPages,
                                         String combinedContext) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are an admissions guidance assistant. Use only the supplied student profile and the supplied public university page body text.\n")
                .append("Do not use background knowledge.\n")
                .append("Do not infer missing requirements.\n")
                .append("Do not invent programme details, APS scores, subject minimums, dates, deadlines, or faculty facts.\n")
                .append("If evidence is missing, write exactly: \"Not found in analysed page content\".\n")
                .append("If the student must confirm something, write exactly: \"Verify on the official university programme page\".\n")
                .append("Ground every recommendation in the supplied evidence only.\n")
                .append("Return strict JSON with fields: summary, recommendedCareers, recommendedProgrammes, recommendedUniversities, minimumRequirements, keyRequirements, skillGaps, recommendedNextSteps, warnings, suitabilityScore.\n")
                .append("Each recommendedCareer object must contain: name, reason, requirements, relatedProgrammes.\n")
                .append("Each recommendedProgramme object must contain: name, university, admissionRequirements, notes.\n\n")
                .append("Student profile:\n")
                .append("- Target programme: ").append(safeText(request.targetProgram(), "not provided")).append("\n")
                .append("- Career interest: ").append(safeText(request.careerInterest(), "not provided")).append("\n")
                .append("- Requested qualification level: ").append(safeText(request.qualificationLevel(), "not provided")).append("\n")
                .append("- Student qualification level: ").append(safeText(profile.getQualificationLevel(), "not provided")).append("\n")
                .append("- Interests: ").append(safeText(profile.getInterests(), "not provided")).append("\n")
                .append("- Skills: ").append(safeText(profile.getSkills(), "not provided")).append("\n")
                .append("- Location: ").append(safeText(profile.getLocation(), "not provided")).append("\n\n")
                .append("Analysed public university pages: ").append(fetchedPages.stream().filter(UniversitySourcePageResult::success).count()).append("\n")
                .append("Visible page body text:\n")
                .append(combinedContext);
        return builder.toString();
    }

    private String recoverJson(String value) {
        String cleaned = stripCodeFences(value);
        if (cleaned.isBlank()) {
            return "";
        }
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return cleaned.substring(firstBrace, lastBrace + 1).trim();
        }
        return cleaned.trim();
    }

    private String stripCodeFences(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private List<String> readStringList(JsonNode root, String fieldName) {
        List<String> values = new ArrayList<>();
        JsonNode field = root.path(fieldName);
        if (field.isArray()) {
            for (JsonNode item : field) {
                String value = item.asText("").trim();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private String textOrDefault(JsonNode root, String fieldName, String defaultValue) {
        String value = root.path(fieldName).asText("").trim();
        return value.isBlank() ? defaultValue : value;
    }

    private Integer normalizeScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String stripBullet(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceFirst("^[-*•]+\\s*", "").trim();
    }

    private String safeText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String readResponseBody(ResponseBody body) throws IOException {
        return body == null ? "" : body.string();
    }

    private void sleepBeforeRetry(int attempt) {
        long backoff = RETRY_BACKOFF_MS[Math.min(attempt - 1, RETRY_BACKOFF_MS.length - 1)];
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars) + "...";
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private GeminiRequestConfig requestConfig() {
        String normalizedBaseUrl = GeminiModelResolver.normalizeBaseUrl(BASE_URL);
        return new GeminiRequestConfig(
                API_KEY == null ? "" : API_KEY.trim(),
                GeminiModelResolver.resolveModelName(MODEL),
                normalizedBaseUrl,
                normalizedBaseUrl + GeminiModelResolver.buildGenerateContentPath(MODEL, BASE_URL),
                normalizedBaseUrl + GeminiModelResolver.buildModelInfoPath(MODEL, BASE_URL)
        );
    }

    public record GeminiHealthCheck(boolean apiKeyPresent,
                                    boolean endpointReachable,
                                    String model,
                                    String endpoint,
                                    String message) {
    }

    private enum RequestMode {
        CAREER_ADVICE,
        UNIVERSITY_GUIDANCE
    }

    private record GeminiRequestConfig(String apiKey,
                                       String model,
                                       String baseUrl,
                                       String generateEndpoint,
                                       String modelInfoEndpoint) {
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
    }

    private static class RecommendedCareerPayload {
        public String name;
        public String reason;
        public List<String> requirements;
        public List<String> relatedProgrammes;
    }

    private static class RecommendedProgrammePayload {
        public String name;
        public String university;
        public List<String> admissionRequirements;
        public String notes;
    }
}
