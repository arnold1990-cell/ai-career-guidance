package com.edurite.subscription.repository;

import com.edurite.subscription.entity.SubscriptionRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionRecord, UUID> {
    Optional<SubscriptionRecord> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}
