package com.edurite.admin.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog extends BaseEntity {
    private UUID actorId;

    @Column(nullable = false)
    private String action;

    private String entityType;
    private UUID entityId;

    @Column(columnDefinition = "TEXT")
    private String details;
}
