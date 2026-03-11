package com.edurite.career.repository; // declares the package path for this Java file

import com.edurite.career.entity.Career; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.domain.Page; // imports a class so it can be used in this file
import org.springframework.data.domain.Pageable; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.Query; // imports a class so it can be used in this file
import org.springframework.data.repository.query.Param; // imports a class so it can be used in this file

/**
 * This interface named CareerRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface CareerRepository extends JpaRepository<Career, UUID> { // defines an interface contract
    @Query(""" // adds metadata that Spring or Java uses at runtime
            SELECT c FROM Career c // supports the surrounding application logic
            WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%')) // supports the surrounding application logic
              AND LOWER(COALESCE(c.industry, '')) LIKE LOWER(CONCAT('%', :industry, '%')) // supports the surrounding application logic
              AND LOWER(COALESCE(c.qualificationLevel, '')) LIKE LOWER(CONCAT('%', :qualificationLevel, '%')) // supports the surrounding application logic
              AND LOWER(COALESCE(c.location, '')) LIKE LOWER(CONCAT('%', :location, '%')) // supports the surrounding application logic
              AND LOWER(COALESCE(c.demandLevel, '')) LIKE LOWER(CONCAT('%', :demandLevel, '%')) // supports the surrounding application logic
              AND LOWER(COALESCE(c.salaryRange, '')) LIKE LOWER(CONCAT('%', :salaryRange, '%')) // supports the surrounding application logic
            """) // supports the surrounding application logic
    Page<Career> search( // supports the surrounding application logic
            @Param("title") String title, // adds metadata that Spring or Java uses at runtime
            @Param("industry") String industry, // adds metadata that Spring or Java uses at runtime
            @Param("qualificationLevel") String qualificationLevel, // adds metadata that Spring or Java uses at runtime
            @Param("location") String location, // adds metadata that Spring or Java uses at runtime
            @Param("demandLevel") String demandLevel, // adds metadata that Spring or Java uses at runtime
            @Param("salaryRange") String salaryRange, // adds metadata that Spring or Java uses at runtime
            Pageable pageable); // executes this statement as part of the application logic
} // ends the current code block
