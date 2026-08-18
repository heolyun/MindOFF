package com.mindoff.api.repository;

import com.mindoff.api.domain.ReceiptLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptLineRepository extends JpaRepository<ReceiptLine, UUID> {
    List<ReceiptLine> findAllByReceiptIdOrderById(UUID receiptId);
    void deleteAllByReceiptId(UUID receiptId);
}
