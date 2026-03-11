package com.edurite.student.repository; // declares the package path for this Java file

import com.edurite.student.entity.StudentProfile; // imports a class so it can be used in this file
import java.util.Optional; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named StudentProfileRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> { // defines an interface contract
    Optional<StudentProfile> findByUserId(UUID userId); // reads or writes data through the database layer
} // ends the current code block
