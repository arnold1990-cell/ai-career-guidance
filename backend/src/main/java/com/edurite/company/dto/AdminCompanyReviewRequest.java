package com.edurite.company.dto; // declares the package path for this Java file

import jakarta.validation.constraints.NotBlank; // imports a class so it can be used in this file

/**
 * Note: this method handles the "AdminCompanyReviewRequest" step of the feature.
 * It exists to keep this class focused and reusable.
 */
public record AdminCompanyReviewRequest(@NotBlank String notes) { // declares a method that defines behavior for this class
} // ends the current code block
