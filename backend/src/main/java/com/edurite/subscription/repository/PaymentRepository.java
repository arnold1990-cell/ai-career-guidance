package com.edurite.subscription.repository; // declares the package path for this Java file

import com.edurite.subscription.entity.PaymentRecord; // imports a class so it can be used in this file
import java.util.List; // imports a class so it can be used in this file
import java.util.UUID; // imports a class so it can be used in this file
import org.springframework.data.jpa.repository.JpaRepository; // imports a class so it can be used in this file

/**
 * This interface named PaymentRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface PaymentRepository extends JpaRepository<PaymentRecord, UUID> { // defines an interface contract
    List<PaymentRecord> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId); // reads or writes data through the database layer
} // ends the current code block
