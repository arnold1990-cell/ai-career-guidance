package com.edurite.company.mapper; // declares the package path for this Java file

import com.edurite.company.dto.CompanyProfileDto; // imports a class so it can be used in this file
import com.edurite.company.entity.CompanyProfile; // imports a class so it can be used in this file
import org.springframework.stereotype.Component; // imports a class so it can be used in this file

@Component // marks this class as a Spring-managed component bean
/**
 * This class named CompanyProfileMapper is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CompanyProfileMapper { // defines a class type

    /**
     * Note: this method handles the "toDto" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public CompanyProfileDto toDto(CompanyProfile entity) { // declares a method that defines behavior for this class
        return new CompanyProfileDto( // returns a value from this method to the caller
                entity.getId(), // supports the surrounding application logic
                entity.getCompanyName(), // supports the surrounding application logic
                entity.getRegistrationNumber(), // supports the surrounding application logic
                entity.getIndustry(), // supports the surrounding application logic
                entity.getOfficialEmail(), // supports the surrounding application logic
                entity.getMobileNumber(), // supports the surrounding application logic
                entity.getContactPersonName(), // supports the surrounding application logic
                entity.getAddress(), // supports the surrounding application logic
                entity.getWebsite(), // supports the surrounding application logic
                entity.getDescription(), // supports the surrounding application logic
                entity.getStatus(), // supports the surrounding application logic
                entity.isEmailVerified(), // supports the surrounding application logic
                entity.isMobileVerified(), // supports the surrounding application logic
                entity.getReviewedAt(), // supports the surrounding application logic
                entity.getReviewedBy(), // supports the surrounding application logic
                entity.getReviewNotes() // supports the surrounding application logic
        ); // executes this statement as part of the application logic
    } // ends the current code block
} // ends the current code block
