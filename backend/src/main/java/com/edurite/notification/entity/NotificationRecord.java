package com.edurite.notification.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class NotificationRecord extends BaseEntity {
    @Column(nullable = false)
    private UUID userId;
    private String channel;
    private String eventType;
    private String title;
    private String message;
    @Column(name = "is_read")
    private boolean read;
    private OffsetDateTime sentAt;
}
