package com.edurite.recommendation.controller;

import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.service.RecommendationService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController tells Spring this class exposes REST API endpoints.
@RestController
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/recommendations")
/**
 * This class named RecommendationController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/me")
    /**
     * Beginner note: this method handles the "myRecommendations" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public RecommendationResultDto myRecommendations(Principal principal) {
        return recommendationService.generateForStudent(principal);
    }
}
