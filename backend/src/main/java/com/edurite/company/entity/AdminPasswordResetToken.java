package com.edurite.company.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin_password_reset_tokens")
@Getter
@Setter
public class AdminPasswordResetToken extends BaseEntity {
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false, unique = true)
    private String token;
    @Column(nullable = false)
    private OffsetDateTime expiresAt;
    private OffsetDateTime usedAt;

    public boolean isActive() { return usedAt == null && expiresAt != null && expiresAt.isAfter(OffsetDateTime.now()); }
}
