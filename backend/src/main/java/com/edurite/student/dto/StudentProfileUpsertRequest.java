package com.edurite.student.dto; // declares the package path for this Java file

import jakarta.validation.constraints.Size; // imports a class so it can be used in this file
import java.time.LocalDate; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file

public record StudentProfileUpsertRequest( // supports the surrounding application logic
        @Size(max = 100) String firstName, // adds metadata that Spring or Java uses at runtime
        @Size(max = 100) String lastName, // adds metadata that Spring or Java uses at runtime
        String phone, // supports the surrounding application logic
        LocalDate dateOfBirth, // supports the surrounding application logic
        String gender, // supports the surrounding application logic
        String location, // supports the surrounding application logic
        String bio, // supports the surrounding application logic
        String qualificationLevel, // supports the surrounding application logic
        List<String> qualifications, // supports the surrounding application logic
        List<String> experience, // supports the surrounding application logic
        List<String> skills, // supports the surrounding application logic
        List<String> interests, // supports the surrounding application logic
        String careerGoals // supports the surrounding application logic
) {} // supports the surrounding application logic
