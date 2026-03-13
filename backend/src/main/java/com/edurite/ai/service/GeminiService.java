package com.edurite.ai.service;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.exception.AiServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Fallback path used: Gemini API key is missing, returning explicit AI unavailable error.");
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Career AI is currently unavailable. Gemini API key is not configured.");
        }

        String endpoint = GEMINI_BASE_URL + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        String prompt = buildPrompt(request);

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

        log.info("Starting Gemini call: model={}, endpointPath=/v1beta/models/{}:generateContent", model, model);

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
            String modelText = extractModelText(geminiBody);
            return parseCareerAdvice(modelText);
        } catch (AiServiceException ex) {
            throw ex;
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
}
