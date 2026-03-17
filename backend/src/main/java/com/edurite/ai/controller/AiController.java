package com.edurite.ai.controller;

import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.exception.AiServiceException;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.UniversitySourcesGuidanceService;
import com.edurite.ai.university.UniversitySourceCoverage;
import com.edurite.ai.university.UniversitySourceCoverageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final UniversitySourcesGuidanceService universitySourcesGuidanceService;
    private final UniversitySourceCoverageService sourceCoverageService;

    public AiController(GeminiService geminiService,
                        UniversitySourcesGuidanceService universitySourcesGuidanceService,
                        UniversitySourceCoverageService sourceCoverageService) {
        this.geminiService = geminiService;
        this.universitySourcesGuidanceService = universitySourcesGuidanceService;
        this.sourceCoverageService = sourceCoverageService;
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
            return errorResponse(httpRequest, ex);
        }
    }

    @PostMapping("/analyse-university-sources")
    public ResponseEntity<?> analyseUniversitySources(@Valid @RequestBody UniversitySourcesAnalysisRequest request,
                                                      Principal principal,
                                                      HttpServletRequest httpRequest) {
        try {
            UniversitySourcesAnalysisResponse response = universitySourcesGuidanceService.analyse(principal, request);
            return ResponseEntity.ok(response);
        } catch (AiServiceException ex) {
            return errorResponse(httpRequest, ex);
        }
    }

    @GetMapping("/default-university-sources")
    public ResponseEntity<List<String>> defaultUniversitySources() {
        return ResponseEntity.ok(universitySourcesGuidanceService.getDefaultSources());
    }

    @GetMapping("/source-coverage")
    public ResponseEntity<UniversitySourceCoverage> sourceCoverage() {
        return ResponseEntity.ok(sourceCoverageService.getCoverage());
    }

    @GetMapping("/gemini-health")
    public ResponseEntity<GeminiService.GeminiHealthCheck> geminiHealth() {
        return ResponseEntity.ok(geminiService.checkHealth());
    }

    private ResponseEntity<Map<String, Object>> errorResponse(HttpServletRequest httpRequest, AiServiceException ex) {
        log.warn("AI guidance request failed: status={}, message={}", ex.getStatus().value(), ex.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", ex.getStatus().value());
        body.put("error", ex.getStatus().getReasonPhrase());
        body.put("message", "AI guidance is temporarily unavailable. Please try again shortly.");
        body.put("path", httpRequest.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
