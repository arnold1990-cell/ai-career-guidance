package com.edurite.student.repository; // declares the package path for this Java file

import com.edurite.student.entity.SavedCareer; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named SavedCareerRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface SavedCareerRepository extends JpaRepository<SavedCareer, UUID> { // defines an interface contract
    long countByStudentId(UUID studentId); // executes this statement as part of the application logic
    List<SavedCareer> findByStudentId(UUID studentId); // reads or writes data through the database layer
    boolean existsByStudentIdAndCareerId(UUID studentId, UUID careerId); // executes this statement as part of the application logic
    void deleteByStudentIdAndCareerId(UUID studentId, UUID careerId); // reads or writes data through the database layer
} // ends the current code block
