package com.edurite.common.entity; // declares the package path for this Java file

import jakarta.persistence.Column; // imports a class so it can be used in this file
import jakarta.persistence.GeneratedValue; // imports a class so it can be used in this file
import jakarta.persistence.Id; // imports a class so it can be used in this file
import jakarta.persistence.MappedSuperclass; // imports a class so it can be used in this file
import java.time.OffsetDateTime; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import lombok.Getter; // imports a class so it can be used in this file
import lombok.Setter; // imports a class so it can be used in this file
import org.hibernate.annotations.CreationTimestamp; // imports a class so it can be used in this file
import org.hibernate.annotations.UpdateTimestamp; // imports a class so it can be used in this file

@Getter // generates getter methods for fields at compile time
@Setter // generates setter methods for fields at compile time
@MappedSuperclass // adds metadata that Spring or Java uses at runtime
/**
 * This class named BaseEntity is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public abstract class BaseEntity { // defines a class type

// @Id marks the primary key column in the table.
    @Id // marks this field as the primary key in the database table
// @GeneratedValue means the ID value is generated automatically.
    @GeneratedValue // tells JPA to auto-generate this primary key value
    private UUID id; // executes this statement as part of the application logic

    @CreationTimestamp // automatically stores when this database row was created
// @Column configures how this field is stored in the database.
    @Column(nullable = false, updatable = false) // configures how this field maps to a database column
    private OffsetDateTime createdAt; // executes this statement as part of the application logic

    @UpdateTimestamp // automatically stores when this database row was last updated
// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private OffsetDateTime updatedAt; // executes this statement as part of the application logic
} // ends the current code block
