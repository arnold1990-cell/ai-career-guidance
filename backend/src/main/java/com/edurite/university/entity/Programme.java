package com.edurite.university.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "courses")
@Getter
@Setter
public class Programme extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "institution_id")
    private UUID institutionId;

    private String level;
}
