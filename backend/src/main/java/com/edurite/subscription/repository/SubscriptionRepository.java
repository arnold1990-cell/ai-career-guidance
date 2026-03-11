package com.edurite.subscription.repository; // declares the package path for this Java file

import com.edurite.subscription.entity.SubscriptionRecord; // imports a class so it can be used in this file
import java.util.Optional; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named SubscriptionRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface SubscriptionRepository extends JpaRepository<SubscriptionRecord, UUID> { // defines an interface contract
    Optional<SubscriptionRecord> findTopByUserIdOrderByCreatedAtDesc(UUID userId); // executes this statement as part of the application logic
} // ends the current code block
