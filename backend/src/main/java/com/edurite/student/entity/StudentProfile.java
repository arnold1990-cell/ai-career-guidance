package com.edurite.student.entity; // declares the package path for this Java file

import com.edurite.common.entity.BaseEntity; // imports a class so it can be used in this file
import jakarta.persistence.Column; // imports a class so it can be used in this file
import jakarta.persistence.Entity; // imports a class so it can be used in this file
import jakarta.persistence.Table; // imports a class so it can be used in this file
import java.time.LocalDate; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import lombok.Getter; // imports a class so it can be used in this file
import lombok.Setter; // imports a class so it can be used in this file

// @Entity tells JPA that this class maps to a database table.
@Entity // marks this class as a JPA entity that maps to a database table
// @Table configures the exact database table name and options.
@Table(name = "students") // sets database table mapping details for this entity
@Getter // generates getter methods for fields at compile time
@Setter // generates setter methods for fields at compile time
/**
 * This class named StudentProfile is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class StudentProfile extends BaseEntity { // defines a class type

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true) // configures how this field maps to a database column
    private UUID userId; // executes this statement as part of the application logic

    private String firstName; // executes this statement as part of the application logic
    private String lastName; // executes this statement as part of the application logic
    private String interests; // executes this statement as part of the application logic
    private String location; // executes this statement as part of the application logic
    private String phone; // executes this statement as part of the application logic
    private LocalDate dateOfBirth; // executes this statement as part of the application logic
    private String gender; // executes this statement as part of the application logic
    private String bio; // executes this statement as part of the application logic
    private String qualificationLevel; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String qualifications; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String experience; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String skills; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String careerGoals; // executes this statement as part of the application logic

    private String cvFileUrl; // executes this statement as part of the application logic
    private String transcriptFileUrl; // executes this statement as part of the application logic
    private boolean profileCompleted; // executes this statement as part of the application logic
    private boolean inAppNotificationsEnabled = true; // executes this statement as part of the application logic
    private boolean emailNotificationsEnabled = false; // executes this statement as part of the application logic
    private boolean smsNotificationsEnabled = false; // executes this statement as part of the application logic
} // ends the current code block
