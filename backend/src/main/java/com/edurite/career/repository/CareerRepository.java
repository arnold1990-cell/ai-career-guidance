package com.edurite.career.repository;

import com.edurite.career.entity.Career;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * This interface named CareerRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface CareerRepository extends JpaRepository<Career, UUID> {
    @Query("""
            SELECT c FROM Career c
            WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%'))
              AND LOWER(COALESCE(c.industry, '')) LIKE LOWER(CONCAT('%', :industry, '%'))
              AND LOWER(COALESCE(c.qualificationLevel, '')) LIKE LOWER(CONCAT('%', :qualificationLevel, '%'))
              AND LOWER(COALESCE(c.location, '')) LIKE LOWER(CONCAT('%', :location, '%'))
              AND LOWER(COALESCE(c.demandLevel, '')) LIKE LOWER(CONCAT('%', :demandLevel, '%'))
              AND LOWER(COALESCE(c.salaryRange, '')) LIKE LOWER(CONCAT('%', :salaryRange, '%'))
            """)
    Page<Career> search(
            @Param("title") String title,
            @Param("industry") String industry,
            @Param("qualificationLevel") String qualificationLevel,
            @Param("location") String location,
            @Param("demandLevel") String demandLevel,
            @Param("salaryRange") String salaryRange,
            Pageable pageable);
}
