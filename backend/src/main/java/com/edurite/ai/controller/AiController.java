package com.edurite.ai.controller;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.UniversitySourceRegistryService;
import com.edurite.ai.service.UniversitySourcesAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final GeminiService geminiService;
    private final UniversitySourcesAnalysisService universitySourcesAnalysisService;
    private final UniversitySourceRegistryService sourceRegistryService;

    public AiController(GeminiService geminiService,
                        UniversitySourcesAnalysisService universitySourcesAnalysisService,
                        UniversitySourceRegistryService sourceRegistryService) {
        this.geminiService = geminiService;
        this.universitySourcesAnalysisService = universitySourcesAnalysisService;
        this.sourceRegistryService = sourceRegistryService;
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
            return aiError(ex, httpRequest.getRequestURI());
        }
    }

    @PostMapping("/analyse-university-sources")
    public ResponseEntity<?> analyseUniversitySources(@Valid @RequestBody UniversitySourcesAnalysisRequest request,
                                                      Principal principal,
                                                      HttpServletRequest httpRequest) {
        try {
            UniversitySourcesAnalysisResponse response = universitySourcesAnalysisService.analyse(request, principal);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), httpRequest.getRequestURI()));
        } catch (AiServiceException ex) {
            return aiError(ex, httpRequest.getRequestURI());
        }
    }

    @GetMapping("/default-university-sources")
    public ResponseEntity<List<String>> defaultUniversitySources() {
        return ResponseEntity.ok(sourceRegistryService.defaultSources());
    }

    private ResponseEntity<Map<String, Object>> aiError(AiServiceException ex, String path) {
        log.warn("AI guidance request failed: status={}, message={}", ex.getStatus().value(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(errorBody(ex.getStatus(), ex.getMessage(), path));
    }

    private Map<String, Object> errorBody(HttpStatus status, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
