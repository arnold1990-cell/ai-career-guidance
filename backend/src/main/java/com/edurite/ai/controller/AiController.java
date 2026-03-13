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
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/career-advice")
    public ResponseEntity<?> careerAdvice(@Valid @RequestBody CareerAdviceRequest request, HttpServletRequest httpRequest) {
        log.info("AI guidance endpoint hit: path={}, qualificationLevel={}, location={}, interestsLength={}, skillsLength={}",
                httpRequest.getRequestURI(),
                safeValue(request.qualificationLevel()),
                safeValue(request.location()),
                safeLength(request.interests()),
                safeLength(request.skills()));
        try {
            CareerAdviceResponse response = geminiService.getCareerAdvice(request);
            log.info("AI guidance request completed successfully: recommendations={}",
                    response.recommendedCareers() == null ? 0 : response.recommendedCareers().size());
            return ResponseEntity.ok(response);
        } catch (AiServiceException ex) {
            log.warn("AI guidance request failed: status={}, message={}", ex.getStatus().value(), ex.getMessage());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("status", ex.getStatus().value());
            body.put("error", ex.getStatus().getReasonPhrase());
            body.put("message", ex.getMessage());
            body.put("path", httpRequest.getRequestURI());
            return ResponseEntity.status(ex.getStatus()).body(body);
        }
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
