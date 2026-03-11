package com.edurite.company.dto; // declares the package path for this Java file

import java.time.OffsetDateTime; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file

public record CompanyDocumentDto( // supports the surrounding application logic
        UUID id, // supports the surrounding application logic
        String documentType, // supports the surrounding application logic
        String objectKey, // supports the surrounding application logic
        String verificationStatus, // supports the surrounding application logic
        String fileName, // supports the surrounding application logic
        OffsetDateTime createdAt // supports the surrounding application logic
) { // supports the surrounding application logic
} // ends the current code block
