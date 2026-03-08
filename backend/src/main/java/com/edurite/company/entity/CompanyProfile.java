package com.edurite.company.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
    private String name;

    private String industry;
}
