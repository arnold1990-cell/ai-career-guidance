package com.edurite.application.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "applications")
@Getter
@Setter
public class ApplicationRecord extends BaseEntity {

    @Column(nullable = false)
    private UUID bursaryId;

    @Column(nullable = false)
    private UUID studentId;

    @Column(nullable = false)
    private String status;
}
