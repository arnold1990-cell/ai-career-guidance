package com.edurite.bursary.repository;

import com.edurite.bursary.entity.Bursary;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BursaryRepository extends JpaRepository<Bursary, UUID> {
    Page<Bursary> findByTitleContainingIgnoreCaseAndQualificationLevelContainingIgnoreCaseAndLocationContainingIgnoreCaseAndEligibilityContainingIgnoreCase(
            String title,
            String qualificationLevel,
            String location,
            String eligibility,
            Pageable pageable
    );

    List<Bursary> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
