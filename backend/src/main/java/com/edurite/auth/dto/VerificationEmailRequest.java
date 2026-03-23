package com.edurite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerificationEmailRequest(
        @NotBlank(message = "email: Email is required")
        @Email(message = "email: Enter a valid email address")
        String email
) {
}
