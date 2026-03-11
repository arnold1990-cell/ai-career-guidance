package com.edurite.user.entity; // declares the package path for this Java file

import com.edurite.common.entity.BaseEntity; // imports a class so it can be used in this file
import jakarta.persistence.Column; // imports a class so it can be used in this file
import jakarta.persistence.Entity; // imports a class so it can be used in this file
import jakarta.persistence.EnumType; // imports a class so it can be used in this file
import jakarta.persistence.Enumerated; // imports a class so it can be used in this file
import jakarta.persistence.FetchType; // imports a class so it can be used in this file
import jakarta.persistence.JoinColumn; // imports a class so it can be used in this file
import jakarta.persistence.JoinTable; // imports a class so it can be used in this file
import jakarta.persistence.ManyToMany; // imports a class so it can be used in this file
import jakarta.persistence.Table; // imports a class so it can be used in this file
import java.util.HashSet; // imports a class so it can be used in this file
import java.util.Set; // imports a class so it can be used in this file
import lombok.Getter; // imports a class so it can be used in this file
import lombok.Setter; // imports a class so it can be used in this file

// @Entity tells JPA that this class maps to a database table.
@Entity // marks this class as a JPA entity that maps to a database table
// @Table configures the exact database table name and options.
@Table(name = "users") // sets database table mapping details for this entity
@Getter // generates getter methods for fields at compile time
@Setter // generates setter methods for fields at compile time
/**
 * This class named User is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class User extends BaseEntity { // defines a class type

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true) // configures how this field maps to a database column
    private String email; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private String passwordHash; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private String firstName; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private String lastName; // executes this statement as part of the application logic

// @Enumerated stores enum values in a readable form in the database.
    @Enumerated(EnumType.STRING) // stores this enum in the database using the chosen enum format
// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private UserStatus status = UserStatus.PENDING; // executes this statement as part of the application logic

    @ManyToMany(fetch = FetchType.EAGER) // adds metadata that Spring or Java uses at runtime
    @JoinTable( // adds metadata that Spring or Java uses at runtime
            name = "user_roles", // supports the surrounding application logic
            joinColumns = @JoinColumn(name = "user_id"), // supports the surrounding application logic
            inverseJoinColumns = @JoinColumn(name = "role_id") // supports the surrounding application logic
    ) // supports the surrounding application logic
    private Set<Role> roles = new HashSet<>(); // creates a new object instance and stores it in a variable
} // ends the current code block
