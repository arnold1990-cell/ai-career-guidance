package com.edurite.company.entity; // declares the package path for this Java file

import com.edurite.common.entity.BaseEntity; // imports a class so it can be used in this file
import jakarta.persistence.Column; // imports a class so it can be used in this file
import jakarta.persistence.Entity; // imports a class so it can be used in this file
import jakarta.persistence.EnumType; // imports a class so it can be used in this file
import jakarta.persistence.Enumerated; // imports a class so it can be used in this file
import jakarta.persistence.Table; // imports a class so it can be used in this file
import java.time.OffsetDateTime; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import lombok.Getter; // imports a class so it can be used in this file
import lombok.Setter; // imports a class so it can be used in this file

// @Entity tells JPA that this class maps to a database table.
@Entity // marks this class as a JPA entity that maps to a database table
// @Table configures the exact database table name and options.
@Table(name = "companies") // sets database table mapping details for this entity
@Getter // generates getter methods for fields at compile time
@Setter // generates setter methods for fields at compile time
/**
 * This class named CompanyProfile is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CompanyProfile extends BaseEntity { // defines a class type

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true) // configures how this field maps to a database column
    private UUID userId; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private String companyName; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true) // configures how this field maps to a database column
    private String registrationNumber; // executes this statement as part of the application logic

    private String industry; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true) // configures how this field maps to a database column
    private String officialEmail; // executes this statement as part of the application logic

    private String mobileNumber; // executes this statement as part of the application logic
    private String contactPersonName; // executes this statement as part of the application logic
    private String address; // executes this statement as part of the application logic
    private String website; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String description; // executes this statement as part of the application logic

// @Enumerated stores enum values in a readable form in the database.
    @Enumerated(EnumType.STRING) // stores this enum in the database using the chosen enum format
// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private CompanyApprovalStatus status = CompanyApprovalStatus.PENDING; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private boolean emailVerified; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private boolean mobileVerified; // executes this statement as part of the application logic

    private OffsetDateTime reviewedAt; // executes this statement as part of the application logic
    private UUID reviewedBy; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(columnDefinition = "TEXT") // configures how this field maps to a database column
    private String reviewNotes; // executes this statement as part of the application logic
} // ends the current code block
