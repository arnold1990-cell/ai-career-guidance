package com.edurite.bursary.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bursaries")
@Getter
@Setter
public class Bursary extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private UUID companyId;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String fieldOfStudy;
    private String qualificationLevel;
    private LocalDate applicationStartDate;
    private LocalDate applicationEndDate;
    private BigDecimal fundingAmount;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(columnDefinition = "TEXT")
    private String requiredSubjects;

    private String minimumGrade;

    @Column(columnDefinition = "TEXT")
    private String demographics;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    private String status;
}
