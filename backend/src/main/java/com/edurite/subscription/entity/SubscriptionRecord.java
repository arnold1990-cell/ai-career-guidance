package com.edurite.subscription.entity;

import com.edurite.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class SubscriptionRecord extends BaseEntity {
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private String planCode;
    @Column(nullable = false)
    private String status;
    private LocalDate renewalDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String paymentReference;
}
