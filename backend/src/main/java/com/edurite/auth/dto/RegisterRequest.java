package com.edurite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email String email,
        @NotBlank String password
) {
}
