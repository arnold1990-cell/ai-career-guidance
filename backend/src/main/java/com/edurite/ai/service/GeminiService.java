package com.edurite.ai.service;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import okhttp3.HttpUrl;
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

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiService(
            OkHttpClient okHttpClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-1.5-flash}") String model
    ) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public CareerAdviceResponse getCareerAdvice(CareerAdviceRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini API key is not configured");
        }

        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("generativelanguage.googleapis.com")
                .addPathSegment("v1beta")
                .addPathSegment("models")
                .addPathSegment(model + ":generateContent")
                .addQueryParameter("key", apiKey)
                .build();

        String prompt = buildPrompt(request);
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    ))
            ));
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(payload, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = okHttpClient.newCall(httpRequest).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();

                if (response.code() == 401 || response.code() == 403) {
                    log.warn("Gemini authentication rejected with status {}", response.code());
                    throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Gemini API authentication failed");
                }
                if (!response.isSuccessful()) {
                    log.warn("Gemini request failed with status {}", response.code());
                    throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Gemini API request failed");
                }

                return parseCareerAdvice(responseBody);
            }
        } catch (SocketTimeoutException e) {
            log.warn("Gemini request timed out");
            throw new AiServiceException(HttpStatus.GATEWAY_TIMEOUT, "Gemini API request timed out");
        } catch (AiServiceException e) {
            throw e;
        } catch (IOException e) {
            log.error("Gemini request failed due to IO error");
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Gemini API communication failed");
        }
    }

    private CareerAdviceResponse parseCareerAdvice(String geminiResponseBody) throws IOException {
        JsonNode root = objectMapper.readTree(geminiResponseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.isNull() || textNode.asText().isBlank()) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Gemini response did not contain advice content");
        }

        String modelJson = stripCodeFences(textNode.asText().trim());
        CareerAdviceResponse parsed = objectMapper.readValue(modelJson, CareerAdviceResponse.class);

        if (parsed.recommendedCareers() == null || parsed.recommendedCareers().isEmpty()) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "Gemini response format was invalid");
        }

        return parsed;
    }

    private String stripCodeFences(String rawText) {
        if (rawText.startsWith("```") && rawText.endsWith("```")) {
            String withoutStart = rawText.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "");
            return withoutStart.replaceFirst("\\s*```$", "").trim();
        }
        return rawText;
    }

    private String buildPrompt(CareerAdviceRequest request) {
        return """
                You are a career guidance assistant for EduRite.
                Return STRICT JSON only. Do not include markdown, comments, or extra text.
                Use exactly this schema:
                {
                  \"recommendedCareers\": [
                    {
                      \"name\": \"Software Developer\",
                      \"matchScore\": 87,
                      \"reason\": \"Strong fit because of interest in technology and problem solving.\",
                      \"improvements\": [\"Learn Spring Boot\", \"Build more Java projects\"]
                    }
                  ]
                }
                Keep matchScore as an integer between 0 and 100.

                Student input:
                qualificationLevel: %s
                interests: %s
                skills: %s
                location: %s
                """.formatted(
                request.qualificationLevel(),
                request.interests(),
                request.skills(),
                request.location()
        );
    }
}
