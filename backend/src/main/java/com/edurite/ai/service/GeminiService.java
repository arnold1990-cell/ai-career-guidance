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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final long[] RETRY_BACKOFF_MS = {400L, 1_000L, 2_000L};
    private static final int MAX_LOG_BODY = 1_500;
    private static final String API_KEY = "REPLACE_WITH_REAL_GEMINI_API_KEY";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String MODEL = "gemini-2.5-flash";

    private final OkHttpClient okHttpClient;
    private final Gson gson = new Gson();
    private final ObjectMapper objectMapper;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(20))
                .readTimeout(Duration.ofSeconds(45))
                .writeTimeout(Duration.ofSeconds(45))
                .callTimeout(Duration.ofSeconds(70))
                .build();
    }

    public GeminiService(ObjectMapper objectMapper, Object ignored) {
        this(objectMapper);
    }

    @PostConstruct
    void logStartupConfiguration() {
        log.info("Gemini configuration loaded from hardcoded constants: apiKeyPresent={}, keyMask={}, model={}, baseUrl={}",
                !API_KEY.isBlank(), maskSecret(API_KEY), MODEL, BASE_URL);
    }

    public GeminiHealthCheck checkHealth() {
        if (API_KEY.isBlank() || API_KEY.startsWith("REPLACE_WITH_REAL")) {
            return new GeminiHealthCheck(false, false, MODEL, generateEndpoint(),
                    "Gemini API key is hardcoded but still set to the placeholder value.");
        }

        Request request = new Request.Builder()
                .url(modelInfoEndpoint())
                .addHeader("x-goog-api-key", API_KEY)
                .get()
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return new GeminiHealthCheck(true, true, MODEL, modelInfoEndpoint(), "Gemini endpoint reachable.");
            }
            return new GeminiHealthCheck(true, false, MODEL, modelInfoEndpoint(),
                    "Gemini endpoint check failed with status " + response.code() + ": " + trim(readSnippet(response.body())));
        } catch (Exception ex) {
            return new GeminiHealthCheck(true, false, MODEL, modelInfoEndpoint(),
                    "Gemini endpoint check exception: " + ex.getMessage());
        }
    }

    public CareerAdviceResponse getCareerAdvice(CareerAdviceRequest request) {
        ensureConfigured();
        return parseCareerAdvice(invokeGemini(buildPrompt(request), "career-advice", List.of(), List.of(), ""));
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
        List<String> successUrls = safeFetchedPages.stream().filter(UniversitySourcePageResult::success).map(UniversitySourcePageResult::sourceUrl).toList();
        List<String> failedUrls = safeFetchedPages.stream().filter(page -> !page.success()).map(UniversitySourcePageResult::sourceUrl).toList();
        List<String> runtimeWarnings = buildSourceWarnings(safeSourceUrls, safeFetchedPages, safeCombinedContext);

        log.info("University guidance request mode=live-attempt model={} endpoint={} requestedUrls={} fetchedPages={} successfulPages={} failedPages={} combinedContextLength={}",
                MODEL, generateEndpoint(), safeSourceUrls.size(), safeFetchedPages.size(), successUrls.size(), failedUrls.size(), safeCombinedContext.length());

        try {
            ensureConfigured();
            String prompt = buildUniversityPrompt(request, profile, safeFetchedPages, safeCombinedContext);
            String modelText = invokeGemini(prompt, "university-guidance", safeSourceUrls, safeFetchedPages, safeCombinedContext);
            UniversitySourcesAnalysisResponse parsed = parseUniversityAdvice(modelText, safeSourceUrls, successUrls, failedUrls);
            return addWarnings(parsed, runtimeWarnings, failedUrls, null);
        } catch (Exception ex) {
            log.warn("University guidance fallback activated: type={} message={}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
            return fallbackUniversityResponse(request, safeSourceUrls, successUrls, failedUrls,
                    addFallbackReason(runtimeWarnings, ex.getMessage()));
        }
    }

    private void ensureConfigured() {
        if (API_KEY.isBlank() || API_KEY.startsWith("REPLACE_WITH_REAL")) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API is not available because the hardcoded API key is missing or still uses the placeholder value.");
        }
    }

    private String invokeGemini(String prompt, String mode, List<String> requestedUrls,
                                List<UniversitySourcePageResult> fetchedPages, String combinedContext) {
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

        Request request = new Request.Builder()
                .url(generateEndpoint())
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", API_KEY)
                .post(RequestBody.create(gson.toJson(payload), JSON))
                .build();

        log.info("Gemini call request mode={} model={} endpoint={} requestedUrls={} fetchedPages={} combinedContextLength={}",
                mode, MODEL, generateEndpoint(), requestedUrls.size(), fetchedPages.size(), combinedContext.length());

        for (int attempt = 0; attempt <= MAX_GEMINI_RETRIES; attempt++) {
            boolean retryAllowed = attempt < MAX_GEMINI_RETRIES;
            try (Response response = okHttpClient.newCall(request).execute()) {
                int statusCode = response.code();
                String responseBody = readSnippet(response.body());
                log.info("Gemini raw response attempt={} status={} body={}", attempt + 1, statusCode, trim(responseBody));

                if (response.isSuccessful()) {
                    return extractModelText(responseBody);
                }

                boolean shouldRetry = statusCode == 408 || statusCode == 429 || statusCode >= 500;
                if (retryAllowed && shouldRetry) {
                    sleepBeforeRetry(attempt);
                    continue;
                }

                throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                        "Gemini request failed with status " + statusCode + ". " + trim(responseBody));
            } catch (IOException ex) {
                log.warn("Gemini IO failure attempt={} mode={} message={}", attempt + 1, mode, ex.getMessage(), ex);
                if (retryAllowed) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw new AiServiceException(HttpStatus.GATEWAY_TIMEOUT, "Gemini request failed after retries.");
            }
        }

        throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini request failed after retries.");
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
                - Output valid JSON only.
                - Recommend 3 to 5 careers.
                - Keep reasons concise.

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
        String sourceBlocks = fetchedPages.stream()
                .map(page -> "URL: " + page.sourceUrl()
                        + "\nSuccess: " + page.success()
                        + "\nPage type: " + page.pageType()
                        + "\nHeadings: " + String.join(" | ", page.extractedHeadings())
                        + "\nVisible body text:\n" + sanitizePromptValue(page.cleanedText()))
                .reduce("", (left, right) -> left + "\n\n" + right)
                .trim();

        return """
                You are EduRite's university guidance assistant.
                Use ONLY the student profile and analysed public university page content below.
                Return ONLY valid JSON with this exact schema:
                {
                  "recommendedCareers": [{"name":"string","reason":"string","requirements":["string"],"relatedProgrammes":["string"]}],
                  "recommendedProgrammes": [{"name":"string","university":"string","admissionRequirements":["string"],"notes":"string"}],
                  "recommendedUniversities": ["string"],
                  "minimumRequirements": ["string"],
                  "keyRequirements": ["string"],
                  "skillGaps": ["string"],
                  "recommendedNextSteps": ["string"],
                  "warnings": ["string"],
                  "summary": "string",
                  "suitabilityScore": 0
                }

                Hard rules:
                - Use supplied evidence only.
                - Do not use prior knowledge over supplied evidence.
                - Do not infer missing requirements.
                - Do not fabricate programme details.
                - Do not invent APS scores, closing dates, subject minimums, or deadlines.
                - When evidence is missing, say exactly: "Not found in analysed page content".
                - If a detail needs verification, say: "Verify on the official university programme page".
                - Every recommendation must be grounded in analysed page content.
                - Keep the wording student-friendly.
                - warning strings must mention limited evidence when relevant.
                - suitabilityScore must be an integer from 0 to 100.

                Student profile:
                firstName: %s
                lastName: %s
                qualificationLevel: %s
                interests: %s
                skills: %s
                experience: %s
                location: %s

                Request focus:
                targetProgram: %s
                careerInterest: %s
                requestedQualificationLevel: %s

                Analysed sources:
                %s

                Combined visible page body context:
                %s
                """.formatted(
                sanitizePromptValue(profile.getFirstName()),
                sanitizePromptValue(profile.getLastName()),
                sanitizePromptValue(profile.getQualificationLevel()),
                sanitizePromptValue(profile.getInterests()),
                sanitizePromptValue(profile.getSkills()),
                sanitizePromptValue(profile.getExperience()),
                sanitizePromptValue(profile.getLocation()),
                sanitizePromptValue(request.targetProgram()),
                sanitizePromptValue(request.careerInterest()),
                sanitizePromptValue(request.qualificationLevel()),
                sanitizePromptValue(sourceBlocks),
                sanitizePromptValue(combinedContext)
        );
    }

    private String extractModelText(String geminiBody) {
        try {
            JsonObject root = JsonParser.parseString(geminiBody).getAsJsonObject();
            JsonArray candidates = root.has("candidates") ? root.getAsJsonArray("candidates") : null;
            if (candidates == null || candidates.isEmpty()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Gemini returned no candidates.");
            }
            List<String> textParts = new ArrayList<>();
            for (JsonElement candidateElement : candidates) {
                JsonObject candidate = candidateElement.getAsJsonObject();
                JsonObject content = candidate.has("content") ? candidate.getAsJsonObject("content") : null;
                JsonArray parts = content != null && content.has("parts") ? content.getAsJsonArray("parts") : null;
                if (parts == null) {
                    continue;
                }
                for (JsonElement partElement : parts) {
                    JsonObject part = partElement.getAsJsonObject();
                    if (part.has("text")) {
                        String value = part.get("text").getAsString().trim();
                        if (!value.isBlank()) {
                            textParts.add(value);
                        }
                    }
                }
            }
            if (textParts.isEmpty()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Gemini returned no text parts.");
            }
            return stripCodeFences(String.join("\n", textParts));
        } catch (Exception ex) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Gemini returned malformed payload.");
        }
    }

    private CareerAdviceResponse parseCareerAdvice(String modelText) {
        try {
            CareerAdviceResponse response = objectMapper.readValue(extractLikelyJson(modelText), CareerAdviceResponse.class);
            List<CareerAdviceResponse.RecommendedCareer> careers = response.recommendedCareers() == null ? List.of() : response.recommendedCareers();
            return new CareerAdviceResponse(careers.stream().map(item -> new CareerAdviceResponse.RecommendedCareer(
                    item.name(), normalizeScore(item.matchScore()), item.reason(), item.improvements() == null ? List.of() : item.improvements()
            )).toList());
        } catch (Exception ex) {
            return fallbackCareerAdvice(modelText);
        }
    }

    private UniversitySourcesAnalysisResponse parseUniversityAdvice(String modelText,
                                                                    List<String> sourceUrls,
                                                                    List<String> successUrls,
                                                                    List<String> failedUrls) throws JsonProcessingException {
        String recoveredJson = recoverStructuredJson(modelText);
        if (!recoveredJson.isBlank()) {
            try {
                return buildUniversityResponse(objectMapper.readValue(recoveredJson, UniversityModelResponse.class), sourceUrls, successUrls, failedUrls);
            } catch (JsonProcessingException ex) {
                log.warn("Recovered JSON still failed parsing: {}", ex.getOriginalMessage());
            }
        }

        UniversityModelResponse sectionParsed = parseSectionedUniversityAdvice(modelText);
        if (sectionParsed != null) {
            return buildUniversityResponse(sectionParsed, sourceUrls, successUrls, failedUrls);
        }
        throw new JsonProcessingException("Could not parse Gemini university response") {};
    }

    private UniversitySourcesAnalysisResponse buildUniversityResponse(UniversityModelResponse parsed,
                                                                      List<String> sourceUrls,
                                                                      List<String> successUrls,
                                                                      List<String> failedUrls) {
        List<String> minimumRequirements = sanitizeList(parsed.minimumRequirements);
        List<String> keyRequirements = sanitizeList(parsed.keyRequirements);
        return new UniversitySourcesAnalysisResponse(
                true,
                false,
                null,
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                sanitizeSourceBoundValue(parsed.summary),
                toCareerList(parsed.recommendedCareers),
                toProgrammeList(parsed.recommendedProgrammes),
                sanitizeList(parsed.recommendedUniversities),
                minimumRequirements,
                keyRequirements,
                sanitizeList(parsed.skillGaps),
                sanitizeList(parsed.recommendedNextSteps),
                sanitizeList(parsed.warnings),
                normalizeScore(parsed.suitabilityScore),
                MODEL
        );
    }

    private UniversityModelResponse parseSectionedUniversityAdvice(String modelText) {
        if (modelText == null || modelText.isBlank()) {
            return null;
        }
        String normalized = modelText.replace("\r", "");
        UniversityModelResponse response = new UniversityModelResponse();
        response.recommendedCareers = new ArrayList<>();
        response.recommendedProgrammes = new ArrayList<>();
        response.recommendedUniversities = new ArrayList<>();
        response.minimumRequirements = new ArrayList<>();
        response.keyRequirements = new ArrayList<>();
        response.skillGaps = new ArrayList<>();
        response.recommendedNextSteps = new ArrayList<>();
        response.warnings = new ArrayList<>();
        response.summary = "Not found in analysed page content";
        response.suitabilityScore = 55;

        String section = "";
        for (String line : normalized.split("\n")) {
            String trimmed = stripBullet(line.trim());
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.equals("recommended careers") || lower.equals("recommended careers:")) { section = "careers"; continue; }
            if (lower.equals("recommended programmes") || lower.equals("recommended programmes:")) { section = "programmes"; continue; }
            if (lower.equals("recommended universities") || lower.equals("recommended universities:")) { section = "universities"; continue; }
            if (lower.equals("minimum requirements") || lower.equals("minimum requirements:")) { section = "minimum"; continue; }
            if (lower.equals("key requirements") || lower.equals("key requirements:")) { section = "key"; continue; }
            if (lower.equals("skill gaps") || lower.equals("skill gaps:")) { section = "gaps"; continue; }
            if (lower.equals("recommended next steps") || lower.equals("recommended next steps:") || lower.equals("next steps") || lower.equals("next steps:")) { section = "steps"; continue; }
            if (lower.equals("warnings") || lower.equals("warnings:")) { section = "warnings"; continue; }
            if (lower.equals("summary") || lower.equals("summary:")) { section = "summary"; continue; }
            if (trimmed.isBlank()) {
                continue;
            }
            switch (section) {
                case "careers" -> response.recommendedCareers.add(careerPayload(trimmed));
                case "programmes" -> response.recommendedProgrammes.add(programmePayload(trimmed));
                case "universities" -> response.recommendedUniversities.add(trimmed);
                case "minimum" -> response.minimumRequirements.add(trimmed);
                case "key" -> response.keyRequirements.add(trimmed);
                case "gaps" -> response.skillGaps.add(trimmed);
                case "steps" -> response.recommendedNextSteps.add(trimmed);
                case "warnings" -> response.warnings.add(trimmed);
                case "summary" -> response.summary = trimmed;
                default -> { }
            }
        }
        return response.recommendedCareers.isEmpty() && response.recommendedProgrammes.isEmpty() && response.recommendedUniversities.isEmpty()
                ? null : response;
    }

    private UniversitySourcesAnalysisResponse addWarnings(UniversitySourcesAnalysisResponse response,
                                                          List<String> runtimeWarnings,
                                                          List<String> failedUrls,
                                                          String warningMessage) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>(sanitizeList(response.warnings()));
        warnings.addAll(runtimeWarnings);
        if (!failedUrls.isEmpty()) {
            warnings.add("Some public university pages failed to load and were skipped.");
        }
        if (response.totalSourcesUsed() == 0) {
            warnings.add("No public university sources were analysed.");
        }
        return new UniversitySourcesAnalysisResponse(
                response.aiLive(),
                response.fallbackUsed(),
                warningMessage != null ? warningMessage : response.warningMessage(),
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
            List<String> warnings) {
        int max = request.safeMaxRecommendations();
        List<String> mergedWarnings = new ArrayList<>(warnings);
        mergedWarnings.add("Fallback recommendations are not live Gemini internet-grounded guidance.");
        if (successUrls.isEmpty()) {
            mergedWarnings.add("No analysed public university page content was available for grounding.");
        }
        return new UniversitySourcesAnalysisResponse(
                false,
                true,
                warnings.isEmpty() ? "Fallback recommendations were used." : warnings.get(0),
                sourceUrls,
                successUrls,
                failedUrls,
                successUrls.size(),
                "Fallback recommendations are shown because live Gemini guidance could not be completed. Verify every programme detail on the official university programme page.",
                List.of(
                        new UniversitySourcesAnalysisResponse.RecommendedCareer("Career exploration needed", "Fallback mode only. Use this as a starting point, not as sourced advice.", List.of("Not found in analysed page content"), List.of("Not found in analysed page content"))
                ).stream().limit(max).toList(),
                List.of(
                        new UniversitySourcesAnalysisResponse.RecommendedProgramme("Verify target programme on official site", "Fallback recommendations", List.of("Not found in analysed page content"), "Verify on the official university programme page")
                ).stream().limit(max).toList(),
                List.of(),
                List.of("Not found in analysed page content"),
                List.of("Verify on the official university programme page"),
                List.of("A live evidence-based gap analysis was not available in fallback mode."),
                List.of("Retry AI guidance later.", "Open official university admissions and programme pages.", "Compare your subjects and marks with the official requirements you find."),
                mergedWarnings,
                35,
                MODEL
        );
    }

    private List<String> buildSourceWarnings(List<String> sourceUrls,
                                             List<UniversitySourcePageResult> fetchedPages,
                                             String combinedContext) {
        List<String> warnings = new ArrayList<>();
        if (sourceUrls.isEmpty()) {
            warnings.add("No public university source URLs were requested.");
        }
        long successCount = fetchedPages.stream().filter(UniversitySourcePageResult::success).count();
        if (successCount == 0) {
            warnings.add("No public university pages were successfully analysed.");
        }
        if (combinedContext.isBlank()) {
            warnings.add("Recommendations are based on limited public content.");
        }
        return warnings;
    }

    private List<String> addFallbackReason(List<String> warnings, String reason) {
        List<String> merged = new ArrayList<>(warnings);
        merged.add("Fallback used because live Gemini guidance failed: " + sanitizePromptValue(reason));
        return merged;
    }

    private String recoverStructuredJson(String modelText) {
        String likelyJson = extractLikelyJson(modelText);
        if (looksLikeJsonObject(likelyJson)) {
            return likelyJson;
        }
        String repaired = likelyJson
                .replace("\u201c", "\"")
                .replace("\u201d", "\"")
                .replace("\u2018", "'")
                .replace("\u2019", "'");
        return looksLikeJsonObject(repaired) ? repaired : "";
    }

    private boolean looksLikeJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            JsonElement element = JsonParser.parseString(text);
            return element.isJsonObject();
        } catch (Exception ex) {
            return false;
        }
    }

    private String extractLikelyJson(String modelText) {
        String cleaned = stripCodeFences(modelText == null ? "" : modelText).trim();
        int objectStart = cleaned.indexOf('{');
        int objectEnd = cleaned.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return cleaned.substring(objectStart, objectEnd + 1).trim();
        }
        return cleaned;
    }

    private String stripCodeFences(String value) {
        return value == null ? "" : value.replace("```json", "").replace("```", "").trim();
    }

    private String sanitizePromptValue(String value) {
        return value == null || value.isBlank() ? "Not provided" : value.replaceAll("\\s+", " ").trim();
    }

    private String sanitizeSourceBoundValue(String value) {
        String cleaned = sanitizePromptValue(value);
        return cleaned.equals("Not provided") ? "Not found in analysed page content" : cleaned;
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(this::sanitizeSourceBoundValue)
                .distinct()
                .toList();
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedCareer> toCareerList(List<UniversityModelResponse.RecommendedCareerPayload> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(item -> item != null && item.name != null && !item.name.isBlank())
                .map(item -> new UniversitySourcesAnalysisResponse.RecommendedCareer(
                        sanitizeSourceBoundValue(item.name),
                        sanitizeSourceBoundValue(item.reason),
                        sanitizeListWithFallback(item.requirements),
                        sanitizeList(item.relatedProgrammes)
                ))
                .toList();
    }

    private List<UniversitySourcesAnalysisResponse.RecommendedProgramme> toProgrammeList(List<UniversityModelResponse.RecommendedProgrammePayload> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(item -> item != null && item.name != null && !item.name.isBlank())
                .map(item -> new UniversitySourcesAnalysisResponse.RecommendedProgramme(
                        sanitizeSourceBoundValue(item.name),
                        sanitizeSourceBoundValue(item.university),
                        sanitizeListWithFallback(item.admissionRequirements),
                        sanitizeSourceBoundValue(item.notes)
                ))
                .toList();
    }

    private List<String> sanitizeListWithFallback(List<String> values) {
        List<String> sanitized = sanitizeList(values);
        return sanitized.isEmpty() ? List.of("Not found in analysed page content") : sanitized;
    }

    private UniversityModelResponse.RecommendedCareerPayload careerPayload(String value) {
        UniversityModelResponse.RecommendedCareerPayload payload = new UniversityModelResponse.RecommendedCareerPayload();
        payload.name = value;
        payload.reason = "Recovered from a malformed Gemini response. Verify on the official university programme page.";
        payload.requirements = List.of("Not found in analysed page content");
        payload.relatedProgrammes = List.of();
        return payload;
    }

    private UniversityModelResponse.RecommendedProgrammePayload programmePayload(String value) {
        UniversityModelResponse.RecommendedProgrammePayload payload = new UniversityModelResponse.RecommendedProgrammePayload();
        payload.name = value;
        payload.university = "Not found in analysed page content";
        payload.admissionRequirements = List.of("Not found in analysed page content");
        payload.notes = "Verify on the official university programme page";
        return payload;
    }

    private CareerAdviceResponse fallbackCareerAdvice(String modelText) {
        List<CareerAdviceResponse.RecommendedCareer> careers = (modelText == null ? List.<String>of() : modelText.lines().toList()).stream()
                .map(this::stripBullet)
                .filter(line -> !line.isBlank())
                .distinct()
                .limit(5)
                .map(line -> new CareerAdviceResponse.RecommendedCareer(line, 60, "Based on available profile context.", List.of("Review this option with a counsellor.")))
                .sorted(Comparator.comparing(CareerAdviceResponse.RecommendedCareer::name))
                .toList();
        return new CareerAdviceResponse(careers.isEmpty()
                ? List.of(new CareerAdviceResponse.RecommendedCareer("General career exploration", 55, "AI output could not be parsed cleanly.", List.of("Try again later.")))
                : careers);
    }

    private String stripBullet(String value) {
        return value == null ? "" : value.replaceFirst("^[\\-*•\	 ]+", "").trim();
    }

    private int normalizeScore(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    private String readSnippet(ResponseBody responseBody) throws IOException {
        return responseBody == null ? "" : responseBody.string();
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_BACKOFF_MS[Math.min(attempt, RETRY_BACKOFF_MS.length - 1)]);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String trim(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.length() > MAX_LOG_BODY ? cleaned.substring(0, MAX_LOG_BODY) + "..." : cleaned;
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "<empty>";
        }
        if (secret.length() <= 4) {
            return "****";
        }
        return "***" + secret.substring(secret.length() - 4);
    }

    private String generateEndpoint() {
        return BASE_URL + "/v1beta/models/" + MODEL + ":generateContent";
    }

    private String modelInfoEndpoint() {
        return BASE_URL + "/v1beta/models/" + MODEL;
    }

    public record GeminiHealthCheck(boolean apiKeyPresent, boolean endpointReachable, String model, String endpoint, String message) {
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
        }

        private static class RecommendedProgrammePayload {
            public String name;
            public String university;
            public List<String> admissionRequirements;
            public String notes;
        }
    }
}
