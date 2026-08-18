package com.mindoff.api.service;

import com.mindoff.api.domain.ReceiptItemTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "mindoff.receipt.ocr-mode", havingValue = "sample", matchIfMissing = true)
public class SampleReceiptOcrGateway implements ReceiptOcrGateway {
    @Override
    public OcrDraft analyze(String fileName, String contentType, byte[] content) {
        LocalDate today = LocalDate.now();
        return new OcrDraft(
                "영수증 매장",
                today,
                BigDecimal.valueOf(9_800),
                List.of(
                        new OcrLine(
                                "우유",
                                BigDecimal.ONE,
                                BigDecimal.valueOf(2_900),
                                BigDecimal.valueOf(2_900),
                                ReceiptItemTarget.FRIDGE,
                                today.plusDays(7)
                        ),
                        new OcrLine(
                                "주방세제",
                                BigDecimal.ONE,
                                BigDecimal.valueOf(6_900),
                                BigDecimal.valueOf(6_900),
                                ReceiptItemTarget.HOUSEHOLD_ITEM,
                                null
                        )
                )
        );
    }
}
