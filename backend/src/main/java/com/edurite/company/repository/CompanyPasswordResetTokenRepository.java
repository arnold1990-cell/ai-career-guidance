package com.edurite.company.repository; // declares the package path for this Java file

import com.edurite.company.entity.CompanyPasswordResetToken; // imports a class so it can be used in this file
import java.time.OffsetDateTime; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.Optional; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named CompanyPasswordResetTokenRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface CompanyPasswordResetTokenRepository extends JpaRepository<CompanyPasswordResetToken, UUID> { // defines an interface contract
    Optional<CompanyPasswordResetToken> findByToken(String token); // handles authentication or authorization to protect secure access
    List<CompanyPasswordResetToken> findByCompanyIdAndUsedAtIsNull(UUID companyId); // handles authentication or authorization to protect secure access
    void deleteByExpiresAtBefore(OffsetDateTime cutoff); // reads or writes data through the database layer
} // ends the current code block
