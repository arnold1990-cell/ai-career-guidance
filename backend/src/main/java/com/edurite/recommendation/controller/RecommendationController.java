package com.edurite.recommendation.controller; // declares the package path for this Java file

import com.edurite.recommendation.dto.RecommendationResultDto; // imports a class so it can be used in this file
import com.edurite.recommendation.service.RecommendationService; // imports a class so it can be used in this file
import java.security.Principal; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.GetMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RequestMapping; // imports a class so it can be used in this file
import org.springframework.web.bind.annotation.RestController; // imports a class so it can be used in this file

// @RestController tells Spring this class exposes REST API endpoints.
@RestController // marks this class as a REST controller that handles HTTP requests
// @RequestMapping defines the base URL path for endpoints in this controller.
@RequestMapping("/api/v1/recommendations") // sets the base URL path for endpoints in this controller
/**
 * This class named RecommendationController is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class RecommendationController { // defines a class type

    private final RecommendationService recommendationService; // executes this statement as part of the application logic

    public RecommendationController(RecommendationService recommendationService) { // declares a method that defines behavior for this class
        this.recommendationService = recommendationService; // executes this statement as part of the application logic
    } // ends the current code block

// @GetMapping handles HTTP GET requests for reading data.
    @GetMapping("/me") // maps this method to handle HTTP GET requests
    /**
     * Note: this method handles the "myRecommendations" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public RecommendationResultDto myRecommendations(Principal principal) { // declares a method that defines behavior for this class
        return recommendationService.generateForStudent(principal); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
