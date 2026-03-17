package com.edurite.ai.controller;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.service.GeminiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/career-advice")
    public ResponseEntity<?> careerAdvice(@Valid @RequestBody CareerAdviceRequest request, HttpServletRequest httpRequest) {
        try {
            CareerAdviceResponse response = geminiService.getCareerAdvice(request);
            return ResponseEntity.ok(response);
        } catch (AiServiceException ex) {
            log.warn("AI guidance request failed: status={}, message={}", ex.getStatus().value(), ex.getMessage());
            return ResponseEntity.status(ex.getStatus()).body(errorBody(httpRequest.getRequestURI(), ex.getStatus().value(), ex.getStatus().getReasonPhrase(), ex.getMessage()));
        }
    }

    private Map<String, Object> errorBody(String path, int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
