package com.edurite.company.repository;

import com.edurite.company.entity.AdminPasswordResetToken;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminPasswordResetTokenRepository extends JpaRepository<AdminPasswordResetToken, UUID> {
    Optional<AdminPasswordResetToken> findByToken(String token);
    void deleteByExpiresAtBefore(OffsetDateTime now);
}
