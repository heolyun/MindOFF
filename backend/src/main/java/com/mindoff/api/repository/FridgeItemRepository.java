package com.mindoff.api.repository;

import com.mindoff.api.domain.FridgeItem;
import com.mindoff.api.domain.FridgeStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FridgeItemRepository extends JpaRepository<FridgeItem, UUID> {
    List<FridgeItem> findAllByHouseholdIdOrderByExpiresAtAsc(UUID householdId);
    long countByHouseholdIdAndStatusAndExpiresAtLessThanEqual(
            UUID householdId,
            FridgeStatus status,
            LocalDate expiresAt
    );
}
