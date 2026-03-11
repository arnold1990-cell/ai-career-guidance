package com.edurite.company.dto; // declares the package path for this Java file

import jakarta.validation.constraints.NotBlank; // imports a class so it can be used in this file
import jakarta.validation.constraints.NotNull; // imports a class so it can be used in this file
import java.math.BigDecimal; // imports a class so it can be used in this file
import java.time.LocalDate; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file

public record CompanyBursaryUpsertRequest( // supports the surrounding application logic
        @NotBlank String bursaryName, // adds metadata that Spring or Java uses at runtime
        @NotBlank String description, // adds metadata that Spring or Java uses at runtime
        @NotBlank String fieldOfStudy, // adds metadata that Spring or Java uses at runtime
        @NotBlank String academicLevel, // adds metadata that Spring or Java uses at runtime
        @NotNull LocalDate applicationStartDate, // adds metadata that Spring or Java uses at runtime
        @NotNull LocalDate applicationEndDate, // adds metadata that Spring or Java uses at runtime
        @NotNull BigDecimal fundingAmount, // adds metadata that Spring or Java uses at runtime
        String benefits, // supports the surrounding application logic
        List<String> requiredSubjects, // supports the surrounding application logic
        String minimumGrade, // supports the surrounding application logic
        List<String> demographics, // supports the surrounding application logic
        String location, // supports the surrounding application logic
        List<String> eligibilityFilters // supports the surrounding application logic
) { // supports the surrounding application logic
} // ends the current code block
