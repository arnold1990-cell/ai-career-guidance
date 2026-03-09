package com.edurite.company.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminCompanyReviewRequest(@NotBlank String notes) {
}
