package com.mindoff.api.repository;

import com.mindoff.api.domain.Subscription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findAllByUserIdOrderByNextBillingAtAsc(UUID userId);
}
