package com.edurite.ai.bookworm.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record BookwormChatRequest(
        @NotBlank(message = "question is required")
        String question,
        UUID studentProfileId
) {
}
