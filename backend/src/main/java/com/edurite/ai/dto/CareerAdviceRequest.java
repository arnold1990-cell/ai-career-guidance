package com.edurite.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CareerAdviceRequest(
        @NotBlank @Size(max = 80) String qualificationLevel,
        @NotBlank @Size(max = 300) String interests,
        @NotBlank @Size(max = 300) String skills,
        @NotBlank @Size(max = 120) String location
) {
}
