package com.edurite.bursary.repository; // declares the package path for this Java file

import com.edurite.bursary.entity.Bursary; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.domain.Page; // imports a class so it can be used in this file
import org.springframework.data.domain.Pageable; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named BursaryRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface BursaryRepository extends JpaRepository<Bursary, UUID> { // defines an interface contract
    Page<Bursary> findByTitleContainingIgnoreCaseAndQualificationLevelContainingIgnoreCaseAndLocationContainingIgnoreCaseAndEligibilityContainingIgnoreCase( // reads or writes data through the database layer
            String title, // supports the surrounding application logic
            String qualificationLevel, // supports the surrounding application logic
            String location, // supports the surrounding application logic
            String eligibility, // supports the surrounding application logic
            Pageable pageable // supports the surrounding application logic
    ); // executes this statement as part of the application logic

    List<Bursary> findByCompanyIdOrderByCreatedAtDesc(UUID companyId); // reads or writes data through the database layer
} // ends the current code block
