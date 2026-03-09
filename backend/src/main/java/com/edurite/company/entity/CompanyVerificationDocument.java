package com.edurite.company.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_documents")
@Getter
@Setter
public class CompanyVerificationDocument extends BaseEntity {

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String documentType;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private String verificationStatus = "PENDING";

    private String fileName;
    private UUID uploadedBy;
}
