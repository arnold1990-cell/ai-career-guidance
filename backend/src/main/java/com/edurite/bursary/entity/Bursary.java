package com.edurite.bursary.entity; // declares the package path for this Java file

import com.edurite.common.entity.BaseEntity; // imports a class so it can be used in this file
import jakarta.persistence.Column; // imports a class so it can be used in this file
import jakarta.persistence.Entity; // imports a class so it can be used in this file
import jakarta.persistence.Table; // imports a class so it can be used in this file
import java.math.BigDecimal; // imports a class so it can be used in this file
import java.time.LocalDate; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import lombok.Getter; // imports a class so it can be used in this file
import lombok.Setter; // imports a class so it can be used in this file

// @Entity tells JPA that this class maps to a database table.
@Entity // marks this class as a JPA entity that maps to a database table
// @Table configures the exact database table name and options.
@Table(name = "bursaries") // sets database table mapping details for this entity
@Getter // generates getter methods for fields at compile time
@Setter // generates setter methods for fields at compile time
/**
 * This class named Bursary is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class Bursary extends BaseEntity { // defines a class type

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private String title; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private UUID companyId; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String description; // executes this statement as part of the application logic

    private String fieldOfStudy; // executes this statement as part of the application logic
    private String qualificationLevel; // executes this statement as part of the application logic
    private LocalDate applicationStartDate; // executes this statement as part of the application logic
    private LocalDate applicationEndDate; // executes this statement as part of the application logic
    private BigDecimal fundingAmount; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String benefits; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String requiredSubjects; // executes this statement as part of the application logic

    private String minimumGrade; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String demographics; // executes this statement as part of the application logic

    private String location; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String eligibility; // executes this statement as part of the application logic

    private String status; // executes this statement as part of the application logic
} // ends the current code block
