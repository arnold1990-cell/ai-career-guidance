package com.edurite.bursary.repository;

import com.edurite.bursary.entity.Bursary;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BursaryRepository extends JpaRepository<Bursary, UUID> {
    Page<Bursary> findByTitleContainingIgnoreCaseAndQualificationLevelContainingIgnoreCaseAndRegionContainingIgnoreCaseAndEligibilityContainingIgnoreCase(
            String title,
            String qualificationLevel,
            String region,
            String eligibility,
            Pageable pageable
    );
}
