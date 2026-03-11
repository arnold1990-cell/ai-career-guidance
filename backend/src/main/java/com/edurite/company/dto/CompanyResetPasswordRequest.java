package com.edurite.company.dto; // declares the package path for this Java file

import jakarta.validation.constraints.NotBlank; // imports a class so it can be used in this file
import jakarta.validation.constraints.Size; // imports a class so it can be used in this file

public record CompanyResetPasswordRequest( // supports the surrounding application logic
        @NotBlank String token, // adds metadata that Spring or Java uses at runtime
        @NotBlank @Size(min = 8, max = 100) String newPassword, // adds metadata that Spring or Java uses at runtime
        @NotBlank @Size(min = 8, max = 100) String confirmPassword // adds metadata that Spring or Java uses at runtime
) { // supports the surrounding application logic
} // ends the current code block
