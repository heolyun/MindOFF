package com.mindoff.api.repository;

import com.mindoff.api.domain.NeedListItem;
import com.mindoff.api.domain.NeedListStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NeedListItemRepository extends JpaRepository<NeedListItem, UUID> {
    List<NeedListItem> findAllByHouseholdIdOrderByCreatedAtDesc(UUID householdId);
    long countByHouseholdIdAndStatus(UUID householdId, NeedListStatus status);
}
