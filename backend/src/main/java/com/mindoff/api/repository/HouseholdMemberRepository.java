package com.mindoff.api.repository;

import com.mindoff.api.domain.HouseholdMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {
    boolean existsByHouseholdIdAndUserId(UUID householdId, UUID userId);
    Optional<HouseholdMember> findByHouseholdIdAndUserId(UUID householdId, UUID userId);
    Optional<HouseholdMember> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    List<HouseholdMember> findAllByHouseholdIdOrderByCreatedAtAsc(UUID householdId);
}
