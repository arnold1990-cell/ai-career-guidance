package com.edurite.company.dto; // declares the package path for this Java file

import jakarta.validation.constraints.Size; // imports a class so it can be used in this file

public record CompanyProfileUpdateRequest( // supports the surrounding application logic
        @Size(max = 120) String industry, // adds metadata that Spring or Java uses at runtime
        @Size(max = 30) String mobileNumber, // adds metadata that Spring or Java uses at runtime
        @Size(max = 150) String contactPersonName, // adds metadata that Spring or Java uses at runtime
        @Size(max = 255) String address, // adds metadata that Spring or Java uses at runtime
        @Size(max = 255) String website, // adds metadata that Spring or Java uses at runtime
        String description // supports the surrounding application logic
) { // supports the surrounding application logic
} // ends the current code block
