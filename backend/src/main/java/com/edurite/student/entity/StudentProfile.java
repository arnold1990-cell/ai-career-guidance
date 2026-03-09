package com.edurite.student.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
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

    private String firstName;
    private String lastName;
    private String interests;
    private String location;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String bio;
    private String qualificationLevel;

    @Column(columnDefinition = "TEXT")
    private String qualifications;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String careerGoals;

    private String cvFileUrl;
    private String transcriptFileUrl;
    private boolean profileCompleted;
}
