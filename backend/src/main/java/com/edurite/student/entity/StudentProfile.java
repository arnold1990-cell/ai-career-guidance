package com.edurite.student.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "students")
@Getter
@Setter
public class StudentProfile extends BaseEntity {

    @Column(nullable = false, unique = true)
    private UUID userId;

    private String interests;
    private String location;
}
