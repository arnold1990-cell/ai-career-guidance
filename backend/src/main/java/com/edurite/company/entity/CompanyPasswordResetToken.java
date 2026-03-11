package com.edurite.company.entity; // declares the package path for this Java file

import com.edurite.common.entity.BaseEntity; // imports a class so it can be used in this file
import jakarta.persistence.Column; // imports a class so it can be used in this file
import jakarta.persistence.Entity; // imports a class so it can be used in this file
import jakarta.persistence.Table; // imports a class so it can be used in this file
import java.time.OffsetDateTime; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import lombok.Getter; // imports a class so it can be used in this file
import lombok.Setter; // imports a class so it can be used in this file

// @Entity tells JPA that this class maps to a database table.
@Entity // marks this class as a JPA entity that maps to a database table
// @Table configures the exact database table name and options.
@Table(name = "company_password_reset_tokens") // sets database table mapping details for this entity
@Getter // generates getter methods for fields at compile time
@Setter // generates setter methods for fields at compile time
/**
 * This class named CompanyPasswordResetToken is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class CompanyPasswordResetToken extends BaseEntity { // defines a class type

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private UUID companyId; // executes this statement as part of the application logic

// @Column configures how this field is stored in the database.
    @Column(nullable = false, unique = true) // configures how this field maps to a database column
    private String token; // handles authentication or authorization to protect secure access

// @Column configures how this field is stored in the database.
    @Column(nullable = false) // configures how this field maps to a database column
    private OffsetDateTime expiresAt; // executes this statement as part of the application logic

    private OffsetDateTime usedAt; // executes this statement as part of the application logic

    /**
     * Note: this method handles the "isActive" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public boolean isActive() { // declares a method that defines behavior for this class
        return usedAt == null && expiresAt.isAfter(OffsetDateTime.now()); // returns a value from this method to the caller
    } // ends the current code block
} // ends the current code block
