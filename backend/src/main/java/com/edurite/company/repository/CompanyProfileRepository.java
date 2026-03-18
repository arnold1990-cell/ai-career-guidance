package com.edurite.company.repository;

import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface named CompanyProfileRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {
    Optional<CompanyProfile> findByUserId(UUID userId);
    long countByStatus(CompanyApprovalStatus status);
    Optional<CompanyProfile> findByOfficialEmailIgnoreCase(String officialEmail);
    Optional<CompanyProfile> findByMobileNumber(String mobileNumber);
    List<CompanyProfile> findByStatusOrderByCreatedAtAsc(CompanyApprovalStatus status);
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);
}
