package com.edurite.company.repository;

import com.edurite.company.entity.CompanyApprovalStatus;
import com.edurite.company.entity.CompanyProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {
    Optional<CompanyProfile> findByUserId(UUID userId);
    Optional<CompanyProfile> findByOfficialEmailIgnoreCase(String officialEmail);
    Optional<CompanyProfile> findByMobileNumber(String mobileNumber);
    List<CompanyProfile> findByStatusOrderByCreatedAtAsc(CompanyApprovalStatus status);
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);
}
