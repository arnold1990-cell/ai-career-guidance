package com.edurite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRegisterRequest(
        @NotBlank(message = "companyName is required")
        @Size(max = 255, message = "companyName must be at most 255 characters")
        String companyName,
        @Email(message = "email must be a valid email address")
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password,
        @Size(max = 120, message = "industry must be at most 120 characters")
        String industry
) {
}
