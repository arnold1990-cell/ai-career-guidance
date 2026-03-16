package com.edurite.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BookwormChatRequest(
        @NotBlank(message = "message is required")
        @Size(max = 2000, message = "message must be at most 2000 characters")
        String message,
        List<ChatMessage> history
) {

    public record ChatMessage(
            @NotBlank(message = "role is required")
            String role,
            @NotBlank(message = "content is required")
            String content
    ) {
    }
}
