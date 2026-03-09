package com.edurite.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100) String newPassword,
        @NotBlank @Size(min = 8, max = 100) String confirmPassword
) {
}
