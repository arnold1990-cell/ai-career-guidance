package com.edurite.auth.dto; // declares the package path for this Java file

import java.util.Set; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file

public record AuthResponse( // supports the surrounding application logic
        String accessToken, // handles authentication or authorization to protect secure access
        String refreshToken, // handles authentication or authorization to protect secure access
        String tokenType, // handles authentication or authorization to protect secure access
        long accessTokenExpiresIn, // handles authentication or authorization to protect secure access
        UserSummary user // supports the surrounding application logic
) { // supports the surrounding application logic
    /**
     * Note: this method handles the "UserSummary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public record UserSummary(UUID id, String email, String fullName, Set<String> roles) { // declares a method that defines behavior for this class
    } // ends the current code block
} // ends the current code block
