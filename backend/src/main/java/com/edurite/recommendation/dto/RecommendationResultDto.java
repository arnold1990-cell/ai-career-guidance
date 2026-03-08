package com.edurite.recommendation.dto;

public record RecommendationResultDto(String type, String itemId, double score, String rationale, String modelVersion) {
}
