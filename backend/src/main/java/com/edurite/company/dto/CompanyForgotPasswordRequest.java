package com.edurite.company.dto;

/**
 * Beginner note: this method handles the "CompanyForgotPasswordRequest" step of the feature.
 * It exists to keep this class focused and reusable.
 */
public record CompanyForgotPasswordRequest(String email, String mobileNumber) {
}
