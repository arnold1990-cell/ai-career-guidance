package com.edurite.recommendation.dto; // declares the package path for this Java file

import java.util.List; // imports a class so it can be used in this file

public record RecommendationResultDto( // supports the surrounding application logic
        List<RecommendationItemDto> suggestedCareers, // supports the surrounding application logic
        List<RecommendationItemDto> suggestedBursaries, // supports the surrounding application logic
        List<RecommendationItemDto> suggestedCoursesOrImprovements, // supports the surrounding application logic
        List<String> profileImprovementTips, // supports the surrounding application logic
        String modelVersion // supports the surrounding application logic
) { // supports the surrounding application logic
} // ends the current code block
