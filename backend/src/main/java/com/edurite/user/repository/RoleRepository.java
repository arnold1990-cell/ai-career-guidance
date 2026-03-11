package com.edurite.user.repository; // declares the package path for this Java file

import com.edurite.user.entity.Role; // imports a class so it can be used in this file
import java.util.Optional; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named RoleRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> { // defines an interface contract
    Optional<Role> findByName(String name); // reads or writes data through the database layer
} // ends the current code block
