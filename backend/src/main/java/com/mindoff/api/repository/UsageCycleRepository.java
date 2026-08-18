package com.mindoff.api.repository;

import com.mindoff.api.domain.UsageCycle;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageCycleRepository extends JpaRepository<UsageCycle, UUID> {
    List<UsageCycle> findTop3ByHouseholdItemIdOrderByFinishedAtDesc(UUID householdItemId);
}
