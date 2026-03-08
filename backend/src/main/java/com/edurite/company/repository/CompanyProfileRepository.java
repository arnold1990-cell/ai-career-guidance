package com.edurite.company.repository;

import com.edurite.company.entity.CompanyProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {
    Optional<CompanyProfile> findByUserId(UUID userId);
}
