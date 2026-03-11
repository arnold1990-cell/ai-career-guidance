package com.edurite.auth.dto; // declares the package path for this Java file

import jakarta.validation.constraints.Email; // imports a class so it can be used in this file
import jakarta.validation.constraints.NotBlank; // imports a class so it can be used in this file

public record RegisterRequest( // supports the surrounding application logic
        @NotBlank String firstName, // adds metadata that Spring or Java uses at runtime
        @NotBlank String lastName, // adds metadata that Spring or Java uses at runtime
        @Email String email, // adds metadata that Spring or Java uses at runtime
        @NotBlank String password // adds metadata that Spring or Java uses at runtime
) { // supports the surrounding application logic
} // ends the current code block
