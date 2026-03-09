package com.edurite.student.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "saved_bursaries")
@Getter
@Setter
public class SavedBursary extends BaseEntity {
    @Column(nullable = false)
    private UUID studentId;
    @Column(nullable = false)
    private UUID bursaryId;
}
