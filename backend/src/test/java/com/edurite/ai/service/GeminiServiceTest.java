package com.edurite.ai.service;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.exception.AiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiServiceTest {

    @Test
    void getCareerAdviceUsesNormalizedModelInGenerateContentEndpoint() {
        AtomicReference<String> requestUrl = new AtomicReference<>();
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new SuccessResponseInterceptor(requestUrl))
                .build();

        GeminiService service = new GeminiService(new ObjectMapper(), httpClient);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", " /models/gemini-2.0-flash ");

        CareerAdviceResponse response = service.getCareerAdvice(
                new CareerAdviceRequest("BSc", "Technology", "Java", "Cape Town"));

        assertThat(response.recommendedCareers()).hasSize(1);
        assertThat(requestUrl.get())
                .contains("/v1beta/models/gemini-2.0-flash:generateContent")
                .doesNotContain("/models/models/")
                .doesNotContain("/models//models");
    }

    @Test
    void getCareerAdviceFailsFastWhenModelNormalizesToBlank() {
        GeminiService service = new GeminiService(new ObjectMapper(), new OkHttpClient());
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "   ");

        assertThatThrownBy(() -> service.getCareerAdvice(
                new CareerAdviceRequest("BSc", "Technology", "Java", "Cape Town")))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("Gemini model is not configured");
    }

    private static final class SuccessResponseInterceptor implements Interceptor {

        private final AtomicReference<String> requestUrl;

        private SuccessResponseInterceptor(AtomicReference<String> requestUrl) {
            this.requestUrl = requestUrl;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            requestUrl.set(request.url().toString());

            String body = """
                    {
                      \"candidates\": [
                        {
                          \"content\": {
                            \"parts\": [
                              {
                                \"text\": \"{\\\"recommendedCareers\\\":[{\\\"name\\\":\\\"Software Engineer\\\",\\\"matchScore\\\":85,\\\"reason\\\":\\\"Strong technical fit\\\",\\\"improvements\\\":[\\\"Practice system design\\\"]}]}\"
                              }
                            ]
                          }
                        }
                      ]
                    }
                    """;

            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(ResponseBody.create(body, MediaType.get("application/json")))
                    .build();
        }
    }
}
