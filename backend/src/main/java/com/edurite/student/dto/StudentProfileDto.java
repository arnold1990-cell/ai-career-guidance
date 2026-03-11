package com.edurite.student.dto; // declares the package path for this Java file

import java.time.LocalDate; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file

public record StudentProfileDto( // supports the surrounding application logic
        UUID id, // supports the surrounding application logic
        String firstName, // supports the surrounding application logic
        String lastName, // supports the surrounding application logic
        String email, // supports the surrounding application logic
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
        String careerGoals, // supports the surrounding application logic
        String cvFileUrl, // supports the surrounding application logic
        String transcriptFileUrl, // supports the surrounding application logic
        boolean profileCompleted, // supports the surrounding application logic
        int profileCompleteness // supports the surrounding application logic
) {} // supports the surrounding application logic
