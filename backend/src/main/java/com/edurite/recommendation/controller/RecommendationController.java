package com.edurite.recommendation.controller;

import com.edurite.recommendation.dto.RecommendationResultDto;
import com.edurite.recommendation.service.RecommendationService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/me")
    public RecommendationResultDto myRecommendations(Principal principal) {
        return recommendationService.generateForStudent(principal);
    }
}
