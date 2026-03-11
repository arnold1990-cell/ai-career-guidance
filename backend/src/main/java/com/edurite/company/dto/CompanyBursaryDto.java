package com.edurite.company.dto; // declares the package path for this Java file

import java.math.BigDecimal; // imports a class so it can be used in this file
import java.time.LocalDate; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file

public record CompanyBursaryDto( // supports the surrounding application logic
        UUID id, // supports the surrounding application logic
        String bursaryName, // supports the surrounding application logic
        String description, // supports the surrounding application logic
        String fieldOfStudy, // supports the surrounding application logic
        String academicLevel, // supports the surrounding application logic
        LocalDate applicationStartDate, // supports the surrounding application logic
        LocalDate applicationEndDate, // supports the surrounding application logic
        BigDecimal fundingAmount, // supports the surrounding application logic
        String benefits, // supports the surrounding application logic
        String requiredSubjects, // supports the surrounding application logic
        String minimumGrade, // supports the surrounding application logic
        String demographics, // supports the surrounding application logic
        String location, // supports the surrounding application logic
        String eligibility, // supports the surrounding application logic
        String status // supports the surrounding application logic
) { // supports the surrounding application logic
} // ends the current code block
