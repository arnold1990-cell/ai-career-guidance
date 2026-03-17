package com.edurite.ai.service;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.university.UniversitySourcePageResult;
import com.edurite.student.entity.StudentProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final String apiKey;
    private final String configuredModel;

    public GeminiService(ObjectMapper objectMapper, Environment environment) {
        this(
                objectMapper,
                environment.getProperty("gemini.api-key", ""),
                environment.getProperty("gemini.model", "gemini-1.5-flash")
        );
    }

    public GeminiService(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-1.5-flash}") String configuredModel
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.configuredModel = configuredModel == null ? "" : configuredModel.trim();
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(20))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
    }

    public CareerAdviceResponse getCareerAdvice(CareerAdviceRequest request) {
        log.info("Career advice request started: qualificationLevel={}, location={}", request.qualificationLevel(), request.location());

        String key = resolveApiKey();
        if (key.isBlank()) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini API key is not configured.");
        }

        String model = resolveModel();
        String endpoint = "%s/v1beta/models/%s:generateContent?key=%s".formatted(GEMINI_BASE_URL, model, key);
        log.info("Gemini model used: {}", model);

        try {
            String prompt = buildCareerAdvicePrompt(request);
            String payload = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .putArray("contents")
                            .add(objectMapper.createObjectNode()
                                    .putArray("parts")
                                    .add(objectMapper.createObjectNode().put("text", prompt))));

            Request httpRequest = new Request.Builder()
                    .url(endpoint)
                    .post(RequestBody.create(payload, JSON))
                    .build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                int status = response.code();
                String body = response.body() == null ? "" : response.body().string();
                log.info("Gemini HTTP status: {}", status);

                if (!response.isSuccessful()) {
                    throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                            "Gemini request failed with status " + status + ".");
                }

                String modelText = extractModelText(body);
                String cleaned = stripCodeFences(modelText);
                CareerAdviceResponse parsed = objectMapper.readValue(cleaned, CareerAdviceResponse.class);
                log.info("Career advice parse success: recommendations={}",
                        parsed.recommendedCareers() == null ? 0 : parsed.recommendedCareers().size());
                return parsed;
            }
        } catch (AiServiceException ex) {
            log.warn("Career advice parse/fetch failed: {}", ex.getMessage());
            throw ex;
        } catch (IOException ex) {
            log.warn("Career advice parse/fetch failed: {}", ex.getMessage(), ex);
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Unable to fetch or parse Gemini response.");
        }
    }

    public UniversitySourcesAnalysisResponse getUniversitySourcesAdvice(
            UniversitySourcesAnalysisRequest request,
            StudentProfile profile,
            List<String> sourceUrls,
            List<UniversitySourcePageResult> fetchedPages,
            String combinedContext
    ) {
        throw new AiServiceException(HttpStatus.NOT_IMPLEMENTED,
                "University sources AI guidance module is not part of this stable career advice rebuild.");
    }

    public GeminiHealthCheck checkHealth() {
        return new GeminiHealthCheck(!resolveApiKey().isBlank(), resolveModel());
    }

    private String resolveApiKey() {
        return apiKey;
    }

    private String resolveModel() {
        String value = configuredModel.isBlank() ? "gemini-1.5-flash" : configuredModel;
        if (value.contains("/") || value.contains(":")) {
            String normalized = value;
            int idx = normalized.lastIndexOf("models/");
            if (idx >= 0) {
                normalized = normalized.substring(idx + "models/".length());
            }
            if (normalized.contains(":")) {
                normalized = normalized.substring(0, normalized.indexOf(':'));
            }
            if (normalized.contains("?")) {
                normalized = normalized.substring(0, normalized.indexOf('?'));
            }
            value = normalized.trim();
        }
        return value.isBlank() ? "gemini-1.5-flash" : value;
    }

    private String buildCareerAdvicePrompt(CareerAdviceRequest request) {
        return """
                You are EduRite career guidance AI.
                Return STRICT JSON only with no markdown and no explanation.
                Use this schema exactly:
                {
                  "recommendedCareers": [
                    {
                      "name": "string",
                      "matchScore": 0,
                      "reason": "string",
                      "improvements": ["string"]
                    }
                  ]
                }

                Student profile:
                - qualificationLevel: %s
                - interests: %s
                - skills: %s
                - location: %s

                Rules:
                - Provide 5 to 8 recommended careers.
                - matchScore must be integer from 0 to 100.
                - Keep reason concise and practical.
                - improvements must contain 2 to 4 practical actions.
                - Output valid JSON only.
                """.formatted(
                request.qualificationLevel(),
                request.interests(),
                request.skills(),
                request.location()
        );
    }

    private String extractModelText(String geminiBody) {
        try {
            JsonNode root = objectMapper.readTree(geminiBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Gemini returned no generated content.");
            }
            return textNode.asText();
        } catch (IOException ex) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Invalid Gemini response JSON.");
        }
    }

    private String stripCodeFences(String input) {
        String value = input == null ? "" : input.trim();
        if (value.startsWith("```") && value.endsWith("```")) {
            int firstLineBreak = value.indexOf('\n');
            if (firstLineBreak > -1) {
                value = value.substring(firstLineBreak + 1, value.length() - 3).trim();
            }
        }
        return value;
    }

    public record GeminiHealthCheck(boolean apiKeyConfigured, String model) {
    }
}
