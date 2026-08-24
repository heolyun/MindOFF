package com.mindoff.api.repository;

import com.mindoff.api.domain.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findAllByUserIdOrderByNextBillingAtAsc(UUID userId);
    Optional<Subscription> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select subscription from Subscription subscription
            where subscription.userId = :userId
               or (subscription.shared = true and subscription.householdId = :householdId)
            order by subscription.nextBillingAt asc
            """)
    List<Subscription> findVisibleToUser(
            @Param("userId") UUID userId,
            @Param("householdId") UUID householdId
    );
}
