package com.edurite.student.repository;

import com.edurite.student.entity.SavedCareer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface named SavedCareerRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface SavedCareerRepository extends JpaRepository<SavedCareer, UUID> {

    List<SavedCareer> findByStudentId(UUID studentId);

    boolean existsByStudentIdAndCareerId(UUID studentId, UUID careerId);

    boolean existsByStudentIdAndOpportunityTypeAndExternalKey(UUID studentId, String opportunityType, String externalKey);

    void deleteByStudentIdAndCareerId(UUID studentId, UUID careerId);

    void deleteByStudentIdAndOpportunityTypeAndExternalKey(UUID studentId, String opportunityType, String externalKey);

    long countByStudentId(UUID studentId);
}
