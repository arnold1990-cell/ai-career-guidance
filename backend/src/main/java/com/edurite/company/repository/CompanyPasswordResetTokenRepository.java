package com.edurite.company.repository;

import com.edurite.company.entity.CompanyPasswordResetToken;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyPasswordResetTokenRepository extends JpaRepository<CompanyPasswordResetToken, UUID> {
    Optional<CompanyPasswordResetToken> findByToken(String token);
    List<CompanyPasswordResetToken> findByCompanyIdAndUsedAtIsNull(UUID companyId);
    void deleteByExpiresAtBefore(OffsetDateTime cutoff);
}
