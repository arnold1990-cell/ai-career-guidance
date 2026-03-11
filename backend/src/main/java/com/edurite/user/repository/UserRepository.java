package com.edurite.user.repository; // declares the package path for this Java file

import com.edurite.user.entity.User; // imports a class so it can be used in this file
import java.util.Optional; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named UserRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface UserRepository extends JpaRepository<User, UUID> { // defines an interface contract
    Optional<User> findByEmail(String email); // reads or writes data through the database layer

    boolean existsByEmail(String email); // executes this statement as part of the application logic
} // ends the current code block
