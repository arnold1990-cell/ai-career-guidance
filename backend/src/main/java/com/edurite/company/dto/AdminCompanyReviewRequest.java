package com.edurite.company.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Beginner note: this method handles the "AdminCompanyReviewRequest" step of the feature.
 * It exists to keep this class focused and reusable.
 */
public record AdminCompanyReviewRequest(@NotBlank String notes) {
}
