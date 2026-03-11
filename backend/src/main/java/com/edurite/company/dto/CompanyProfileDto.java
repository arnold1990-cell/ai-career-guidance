package com.edurite.company.dto; // declares the package path for this Java file

import com.edurite.company.entity.CompanyApprovalStatus; // imports a class so it can be used in this file
import java.time.OffsetDateTime; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file

public record CompanyProfileDto( // supports the surrounding application logic
        UUID id, // supports the surrounding application logic
        String companyName, // supports the surrounding application logic
        String registrationNumber, // supports the surrounding application logic
        String industry, // supports the surrounding application logic
        String officialEmail, // supports the surrounding application logic
        String mobileNumber, // supports the surrounding application logic
        String contactPersonName, // supports the surrounding application logic
        String address, // supports the surrounding application logic
        String website, // supports the surrounding application logic
        String description, // supports the surrounding application logic
        CompanyApprovalStatus status, // supports the surrounding application logic
        boolean emailVerified, // supports the surrounding application logic
        boolean mobileVerified, // supports the surrounding application logic
        OffsetDateTime reviewedAt, // supports the surrounding application logic
        UUID reviewedBy, // supports the surrounding application logic
        String reviewNotes // supports the surrounding application logic
) { // supports the surrounding application logic
} // ends the current code block
