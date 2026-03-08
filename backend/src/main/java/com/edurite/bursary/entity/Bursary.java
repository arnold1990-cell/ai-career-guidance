package com.edurite.bursary.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    private LocalDate deadline;
    private String status;
}
