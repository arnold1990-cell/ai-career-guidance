package com.edurite.application.repository; // declares the package path for this Java file

import com.edurite.application.entity.ApplicationRecord; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named ApplicationRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface ApplicationRepository extends JpaRepository<ApplicationRecord, UUID> { // defines an interface contract

    long countByStudentId(UUID studentId); // executes this statement as part of the application logic

    long countByStudentIdAndStatus(UUID studentId, String status); // executes this statement as part of the application logic

    List<ApplicationRecord> findByStudentIdOrderByCreatedAtDesc(UUID studentId); // reads or writes data through the database layer
} // ends the current code block