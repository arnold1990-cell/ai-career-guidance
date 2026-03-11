package com.edurite.student.repository; // declares the package path for this Java file

import com.edurite.student.entity.SavedBursary; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named SavedBursaryRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface SavedBursaryRepository extends JpaRepository<SavedBursary, UUID> { // defines an interface contract
    long countByStudentId(UUID studentId); // executes this statement as part of the application logic
    List<SavedBursary> findByStudentId(UUID studentId); // reads or writes data through the database layer
    boolean existsByStudentIdAndBursaryId(UUID studentId, UUID bursaryId); // executes this statement as part of the application logic
    void deleteByStudentIdAndBursaryId(UUID studentId, UUID bursaryId); // reads or writes data through the database layer
} // ends the current code block
