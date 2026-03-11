package com.edurite.student.entity; // declares the package path for this Java file

import com.edurite.common.entity.BaseEntity; // imports a class so it can be used in this file
import jakarta.persistence.Column; // imports a class so it can be used in this file
import jakarta.persistence.Entity; // imports a class so it can be used in this file
import jakarta.persistence.Table; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import lombok.Getter; // imports a class so it can be used in this file
import lombok.Setter; // imports a class so it can be used in this file

// @Entity tells JPA that this class maps to a database table.
@Entity // marks this class as a JPA entity that maps to a database table
// @Table configures the exact database table name and options.
@Table(name = "saved_bursaries") // sets database table mapping details for this entity
@Getter // generates getter methods for fields at compile time
@Setter // generates setter methods for fields at compile time
/**
 * This class named SavedBursary is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class SavedBursary extends BaseEntity { // defines a class type
// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private UUID studentId; // executes this statement as part of the application logic
// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private UUID bursaryId; // executes this statement as part of the application logic
} // ends the current code block
