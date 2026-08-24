package com.mindoff.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindoff.api.domain.ReceiptItemTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TesseractReceiptParserTests {
    private final TesseractReceiptParser parser = new TesseractReceiptParser();

    @Test
    void parsesKoreanReceiptSummaryAndItems() {
        String text = """
                이마트
                2026-08-24 14:30
                우유 2,900
                주방세제 6,900
                합계 9,800원
                카드결제 9,800원
                """;

        ReceiptOcrGateway.OcrDraft draft = parser.parse(text);

        assertThat(draft.merchantName()).isEqualTo("이마트");
        assertThat(draft.purchasedAt()).isEqualTo(LocalDate.of(2026, 8, 24));
        assertThat(draft.totalAmount()).isEqualByComparingTo(new BigDecimal("9800"));
        assertThat(draft.lines()).hasSize(2);
        assertThat(draft.lines().get(0).name()).isEqualTo("우유");
        assertThat(draft.lines().get(0).targetType()).isEqualTo(ReceiptItemTarget.FRIDGE);
        assertThat(draft.lines().get(1).name()).isEqualTo("주방세제");
        assertThat(draft.lines().get(1).targetType()).isEqualTo(ReceiptItemTarget.HOUSEHOLD_ITEM);
    }

    @Test
    void usesItemSumWhenReceiptHasNoTotalLine() {
        String text = """
                동네마트
                사과 3,000
                생수 1,200
                """;

        ReceiptOcrGateway.OcrDraft draft = parser.parse(text);

        assertThat(draft.totalAmount()).isEqualByComparingTo(new BigDecimal("4200"));
        assertThat(draft.lines()).extracting(ReceiptOcrGateway.OcrLine::name)
                .containsExactly("사과", "생수");
    }

    @Test
    void filtersPaymentAndBusinessMetadata() {
        String text = """
                테스트상점
                사업자번호 123-45-67890
                승인번호 12345678
                치약 4,500
                부가세 409
                총액 4,500
                """;

        ReceiptOcrGateway.OcrDraft draft = parser.parse(text);

        assertThat(draft.lines()).singleElement()
                .satisfies(line -> {
                    assertThat(line.name()).isEqualTo("치약");
                    assertThat(line.lineTotal()).isEqualByComparingTo("4500");
                });
    }
}
