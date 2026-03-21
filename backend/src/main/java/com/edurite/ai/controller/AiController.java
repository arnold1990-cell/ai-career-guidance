package com.edurite.ai.controller;

import com.edurite.ai.dto.AiDashboardSummaryResponse;
import com.edurite.ai.dto.CareerAdviceRequest;
import com.edurite.ai.dto.CareerAdviceResponse;
import com.edurite.ai.dto.UniversitySourcesAnalysisRequest;
import com.edurite.ai.dto.UniversitySourcesAnalysisResponse;
import com.edurite.ai.service.GeminiService;
import com.edurite.ai.service.StudentAiGuidanceService;
import com.edurite.ai.service.UniversitySourcesGuidanceService;
import com.edurite.ai.university.UniversitySourceCoverage;
import com.edurite.ai.university.UniversitySourceCoverageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
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
    private final StudentAiGuidanceService studentAiGuidanceService;

    public AiController(GeminiService geminiService,
                        UniversitySourcesGuidanceService universitySourcesGuidanceService,
                        UniversitySourceCoverageService sourceCoverageService,
                        StudentAiGuidanceService studentAiGuidanceService) {
        this.geminiService = geminiService;
        this.universitySourcesGuidanceService = universitySourcesGuidanceService;
        this.sourceCoverageService = sourceCoverageService;
        this.studentAiGuidanceService = studentAiGuidanceService;
    }

    @PostMapping("/career-advice")
    public ResponseEntity<CareerAdviceResponse> careerAdvice(@Valid @RequestBody CareerAdviceRequest request, HttpServletRequest httpRequest) {
        log.info("AI guidance endpoint hit: path={}, qualificationLevel={}, location={}, interestsLength={}, skillsLength={}",
                httpRequest.getRequestURI(),
                safeValue(request.qualificationLevel()),
                safeValue(request.location()),
                safeLength(request.interests()),
                safeLength(request.skills()));
        CareerAdviceResponse response = geminiService.getCareerAdvice(request);
        log.info("AI guidance request completed successfully: recommendations={}",
                response.recommendedCareers() == null ? 0 : response.recommendedCareers().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/career-advice/me")
    public ResponseEntity<CareerAdviceResponse> careerAdviceForStudent(Principal principal) {
        return ResponseEntity.ok(studentAiGuidanceService.careerAdviceForStudent(principal));
    }

    @GetMapping("/bursary-guidance/me")
    public ResponseEntity<List<com.edurite.bursary.dto.BursaryResultDto>> bursaryGuidanceForStudent(Principal principal) {
        return ResponseEntity.ok(studentAiGuidanceService.bursaryGuidanceForStudent(principal));
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<AiDashboardSummaryResponse> dashboardSummary(Principal principal) {
        return ResponseEntity.ok(studentAiGuidanceService.dashboardSummary(principal));
    }

    @PostMapping("/analyse-university-sources")
    public ResponseEntity<UniversitySourcesAnalysisResponse> analyseUniversitySources(@Valid @RequestBody UniversitySourcesAnalysisRequest request,
                                                                                      Principal principal,
                                                                                      HttpServletRequest httpRequest) {
        log.info("University analysis request received: path={}, usesDefaultSources={}, requestedUrls={}, targetProgramPresent={}, careerInterestPresent={}, qualificationLevel={}",
                httpRequest.getRequestURI(),
                request.usesDefaultSources(),
                request.urls() == null ? 0 : request.urls().size(),
                request.targetProgram() != null && !request.targetProgram().isBlank(),
                request.careerInterest() != null && !request.careerInterest().isBlank(),
                safeValue(request.qualificationLevel()));
        return ResponseEntity.ok(universitySourcesGuidanceService.analyse(principal, request));
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

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
