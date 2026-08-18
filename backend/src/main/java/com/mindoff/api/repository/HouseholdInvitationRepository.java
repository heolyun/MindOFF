package com.mindoff.api.repository;

import com.mindoff.api.domain.HouseholdInvitation;
import com.mindoff.api.domain.HouseholdInvitationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdInvitationRepository extends JpaRepository<HouseholdInvitation, UUID> {
    Optional<HouseholdInvitation> findByToken(String token);
    List<HouseholdInvitation> findAllByHouseholdIdOrderByCreatedAtDesc(UUID householdId);
    Optional<HouseholdInvitation> findFirstByHouseholdIdAndEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
            UUID householdId,
            String email,
            HouseholdInvitationStatus status
    );
}
