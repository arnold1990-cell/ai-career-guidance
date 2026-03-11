package com.edurite.notification.repository;

import com.edurite.notification.entity.NotificationRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * This interface named NotificationRepository is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public interface NotificationRepository extends JpaRepository<NotificationRecord, UUID> {
    List<NotificationRecord> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndReadFalse(UUID userId);
}
