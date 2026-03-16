package com.edurite.university.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "entry_requirements")
@Getter
@Setter
public class EntryRequirement extends BaseEntity {

    @Column(name = "programme_id", nullable = false)
    private UUID programmeId;

    @Column(name = "requirement_text", nullable = false, columnDefinition = "TEXT")
    private String requirementText;

    @Column(name = "subject_requirements", columnDefinition = "TEXT")
    private String subjectRequirements;
}
