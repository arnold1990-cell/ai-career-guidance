package com.edurite.auth.dto;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        UserSummary user
) {
    public record UserSummary(UUID id, String email, String fullName, Set<String> roles) {
    }
}
