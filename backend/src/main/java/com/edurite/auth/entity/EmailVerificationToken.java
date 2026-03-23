package com.edurite.auth.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import com.edurite.user.entity.User;

@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
public class EmailVerificationToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    private OffsetDateTime usedAt;
}
