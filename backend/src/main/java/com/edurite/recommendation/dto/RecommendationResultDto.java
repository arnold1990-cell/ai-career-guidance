package com.edurite.recommendation.dto;

public record RecommendationResultDto(String id, String type, String title, int score, String rationale, String modelVersion) {
}
