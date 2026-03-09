package com.edurite.company.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@Setter
public class CompanyProfile extends BaseEntity {

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    private String industry;

    @Column(nullable = false, unique = true)
    private String officialEmail;

    private String mobileNumber;
    private String contactPersonName;
    private String address;
    private String website;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyApprovalStatus status = CompanyApprovalStatus.PENDING;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false)
    private boolean mobileVerified;

    private OffsetDateTime reviewedAt;
    private UUID reviewedBy;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;
}
