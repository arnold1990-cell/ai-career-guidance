package com.edurite.notification.repository; // declares the package path for this Java file

import com.edurite.notification.entity.NotificationRecord; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named NotificationRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface NotificationRepository extends JpaRepository<NotificationRecord, UUID> { // defines an interface contract
    List<NotificationRecord> findByUserIdOrderByCreatedAtDesc(UUID userId); // reads or writes data through the database layer
    long countByUserIdAndReadFalse(UUID userId); // executes this statement as part of the application logic
} // ends the current code block
