package com.edurite.subscription.repository;

import com.edurite.subscription.entity.PaymentRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentRecord, UUID> {
    List<PaymentRecord> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);
}
