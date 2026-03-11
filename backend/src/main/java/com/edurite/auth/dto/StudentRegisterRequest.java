package com.edurite.auth.dto; // declares the package path for this Java file

import jakarta.validation.constraints.Email; // imports a class so it can be used in this file
import jakarta.validation.constraints.NotBlank; // imports a class so it can be used in this file
import jakarta.validation.constraints.Size; // imports a class so it can be used in this file

public record StudentRegisterRequest( // supports the surrounding application logic
        @NotBlank(message = "fullName is required") // adds metadata that Spring or Java uses at runtime
        @Size(max = 200, message = "fullName must be at most 200 characters") // adds metadata that Spring or Java uses at runtime
        String fullName, // supports the surrounding application logic
        @Email(message = "email must be a valid email address") // adds metadata that Spring or Java uses at runtime
        @NotBlank(message = "email is required") // adds metadata that Spring or Java uses at runtime
        String email, // supports the surrounding application logic
        @NotBlank(message = "password is required") // adds metadata that Spring or Java uses at runtime
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters") // adds metadata that Spring or Java uses at runtime
        String password // supports the surrounding application logic
) { // supports the surrounding application logic
} // ends the current code block
