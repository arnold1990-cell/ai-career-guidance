package com.edurite.auth.dto;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        String role,
        String primaryRole,
        UserSummary user
) {
    /**
     * this method handles the "UserSummary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public record UserSummary(UUID id, String email, String fullName, String companyName, Set<String> roles, String role, String primaryRole, String approvalStatus) {
    }
}
