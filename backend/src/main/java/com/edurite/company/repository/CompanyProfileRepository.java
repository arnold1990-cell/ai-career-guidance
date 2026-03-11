package com.edurite.company.repository; // declares the package path for this Java file

import com.edurite.company.entity.CompanyApprovalStatus; // imports a class so it can be used in this file
import com.edurite.company.entity.CompanyProfile; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.Optional; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named CompanyProfileRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> { // defines an interface contract
    Optional<CompanyProfile> findByUserId(UUID userId); // reads or writes data through the database layer
    Optional<CompanyProfile> findByOfficialEmailIgnoreCase(String officialEmail); // reads or writes data through the database layer
    Optional<CompanyProfile> findByMobileNumber(String mobileNumber); // reads or writes data through the database layer
    List<CompanyProfile> findByStatusOrderByCreatedAtAsc(CompanyApprovalStatus status); // reads or writes data through the database layer
    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber); // executes this statement as part of the application logic
} // ends the current code block
