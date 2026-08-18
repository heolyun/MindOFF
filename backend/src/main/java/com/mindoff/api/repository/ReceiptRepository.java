package com.mindoff.api.repository;

import com.mindoff.api.domain.Receipt;
import com.mindoff.api.domain.ReceiptStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findAllByHouseholdIdOrderByCreatedAtDesc(UUID householdId);
    List<Receipt> findAllByHouseholdIdAndStatus(UUID householdId, ReceiptStatus status);
    List<Receipt> findAllByHouseholdIdAndStatusAndPurchasedAtBetween(
            UUID householdId,
            ReceiptStatus status,
            LocalDate start,
            LocalDate end
    );
}
