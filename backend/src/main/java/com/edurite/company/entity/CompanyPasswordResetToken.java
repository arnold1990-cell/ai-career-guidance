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
@Table(name = "company_password_reset_tokens")
@Getter
@Setter
public class CompanyPasswordResetToken extends BaseEntity {

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    private OffsetDateTime usedAt;

    public boolean isActive() {
        return usedAt == null && expiresAt.isAfter(OffsetDateTime.now());
    }
}
