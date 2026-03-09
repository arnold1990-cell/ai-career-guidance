package com.edurite.company.repository;

import com.edurite.company.entity.CompanyVerificationDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyVerificationDocumentRepository extends JpaRepository<CompanyVerificationDocument, UUID> {
    List<CompanyVerificationDocument> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
