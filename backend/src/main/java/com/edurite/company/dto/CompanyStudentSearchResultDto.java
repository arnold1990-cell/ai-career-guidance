package com.edurite.company.dto; // declares the package path for this Java file

import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file

public record CompanyStudentSearchResultDto( // supports the surrounding application logic
        UUID studentId, // supports the surrounding application logic
        String firstName, // supports the surrounding application logic
        String lastName, // supports the surrounding application logic
        String location, // supports the surrounding application logic
        String qualificationLevel, // supports the surrounding application logic
        List<String> skills, // supports the surrounding application logic
        List<String> interests // supports the surrounding application logic
) { // supports the surrounding application logic
} // ends the current code block
