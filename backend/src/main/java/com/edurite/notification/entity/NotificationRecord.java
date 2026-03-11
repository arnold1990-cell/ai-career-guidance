package com.edurite.notification.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// @Entity tells JPA that this class maps to a database table.
@Entity
// @Table configures the exact database table name and options.
@Table(name = "notifications")
@Getter
@Setter
/**
 * This class named NotificationRecord is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class NotificationRecord extends BaseEntity {
// @Column configures how this field is stored in the database.
    @Column(nullable = false)
    private UUID userId;
    private String channel;
    private String eventType;
    private String title;
    private String message;
// @Column configures how this field is stored in the database.
    @Column(name = "is_read")
    private boolean read;
    private OffsetDateTime sentAt;
}
