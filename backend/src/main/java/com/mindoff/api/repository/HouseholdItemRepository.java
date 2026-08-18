package com.mindoff.api.repository;

import com.mindoff.api.domain.HouseholdItem;
import com.mindoff.api.domain.HouseholdItemStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdItemRepository extends JpaRepository<HouseholdItem, UUID> {
    List<HouseholdItem> findAllByHouseholdIdOrderByCreatedAtDesc(UUID householdId);
    List<HouseholdItem> findTop3ByHouseholdIdAndNameIgnoreCaseAndStatusOrderByFinishedAtDesc(
            UUID householdId,
            String name,
            HouseholdItemStatus status
    );
}
