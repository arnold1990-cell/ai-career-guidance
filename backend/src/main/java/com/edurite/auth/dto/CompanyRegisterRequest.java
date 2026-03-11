package com.edurite.auth.dto; // declares the package path for this Java file

import jakarta.validation.constraints.Email; // imports a class so it can be used in this file
import jakarta.validation.constraints.NotBlank; // imports a class so it can be used in this file
import jakarta.validation.constraints.Size; // imports a class so it can be used in this file

public record CompanyRegisterRequest( // supports the surrounding application logic
        @NotBlank @Size(max = 200) String companyName, // adds metadata that Spring or Java uses at runtime
        @Size(max = 120) String registrationNumber, // adds metadata that Spring or Java uses at runtime
        @Size(max = 120) String industry, // adds metadata that Spring or Java uses at runtime
        @Email String officialEmail, // adds metadata that Spring or Java uses at runtime
        @Email String email, // adds metadata that Spring or Java uses at runtime
        @Size(max = 30) String mobileNumber, // adds metadata that Spring or Java uses at runtime
        @Size(max = 150) String contactPersonName, // adds metadata that Spring or Java uses at runtime
        @Size(max = 255) String address, // adds metadata that Spring or Java uses at runtime
        @Size(max = 255) String website, // adds metadata that Spring or Java uses at runtime
        @Size(max = 1000) String description, // adds metadata that Spring or Java uses at runtime
        @NotBlank @Size(min = 8, max = 255) String password // adds metadata that Spring or Java uses at runtime
) {} // supports the surrounding application logic