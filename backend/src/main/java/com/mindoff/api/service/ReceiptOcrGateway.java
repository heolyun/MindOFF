package com.mindoff.api.service;

import com.mindoff.api.domain.ReceiptItemTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ReceiptOcrGateway {
    OcrDraft analyze(String fileName, String contentType, byte[] content);

    record OcrDraft(String merchantName, LocalDate purchasedAt, BigDecimal totalAmount, List<OcrLine> lines) {
    }

    record OcrLine(
            String name,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            ReceiptItemTarget targetType,
            LocalDate expiresAt
    ) {
    }
}
