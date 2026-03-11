package com.edurite.recommendation.dto; // declares the package path for this Java file

/**
 * Note: this method handles the "RecommendationItemDto" step of the feature.
 * It exists to keep this class focused and reusable.
 */
public record RecommendationItemDto(String id, String title, int score, String rationale) { // declares a method that defines behavior for this class
} // ends the current code block
