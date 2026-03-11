package com.edurite.company.repository; // declares the package path for this Java file

import com.edurite.company.entity.CompanyVerificationDocument; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named CompanyVerificationDocumentRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface CompanyVerificationDocumentRepository extends JpaRepository<CompanyVerificationDocument, UUID> { // defines an interface contract
    List<CompanyVerificationDocument> findByCompanyIdOrderByCreatedAtDesc(UUID companyId); // reads or writes data through the database layer
} // ends the current code block
