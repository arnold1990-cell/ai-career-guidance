package com.edurite.ai.dto;

/**
 * API response from the guidance endpoint.
 */
public record AiGuidanceResponse(
        String guidance,
        boolean fallbackUsed,
        String source,
        String message
) {
}
