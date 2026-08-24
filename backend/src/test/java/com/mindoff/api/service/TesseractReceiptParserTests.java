package com.mindoff.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindoff.api.domain.ReceiptItemTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Test
    void filtersSpacedAndMisrecognizedSummaryLines() {
        String text = """
                압 계 수 랑 / 금 액 18,039
                관세 매출 8,436
                생수 790
                """;

        ReceiptOcrGateway.OcrDraft draft = parser.parse(text);

        assertThat(draft.lines()).singleElement()
                .satisfies(line -> {
                    assertThat(line.name()).isEqualTo("생수");
                    assertThat(line.lineTotal()).isEqualByComparingTo("790");
                });
    }

    @Test
    void separatesQuantityUnitPriceAndTotalUsingWordColumns() {
        TesseractDocument document = new TesseractDocument(4, List.of(new TesseractDocument.Line(List.of(
                word("삼겹살", 20, 91),
                word("2", 260, 88),
                word("7,000", 360, 94),
                word("14,000", 500, 96)
        ))));

        ReceiptOcrGateway.OcrDraft draft = parser.parse(document);

        assertThat(draft.lines()).singleElement().satisfies(line -> {
            assertThat(line.name()).isEqualTo("삼겹살");
            assertThat(line.quantity()).isEqualByComparingTo("2");
            assertThat(line.unitPrice()).isEqualByComparingTo("7000");
            assertThat(line.lineTotal()).isEqualByComparingTo("14000");
        });
    }

    @Test
    void interpretsDotsAsWonThousandsSeparators() {
        TesseractDocument document = new TesseractDocument(4, List.of(new TesseractDocument.Line(List.of(
                word("맥주", 20, 90),
                word("3.000", 360, 92),
                word("12.000", 500, 95)
        ))));

        ReceiptOcrGateway.OcrDraft draft = parser.parse(document);

        assertThat(draft.lines()).singleElement().satisfies(line -> {
            assertThat(line.name()).isEqualTo("맥주");
            assertThat(line.lineTotal()).isEqualByComparingTo("12000");
        });
    }

    @Test
    void ignoresImplausiblyOldRecognizedDate() {
        ReceiptOcrGateway.OcrDraft draft = parser.parse("""
                이마트
                2013-09-22
                우유 2,900
                합계 2,900
                """);

        assertThat(draft.purchasedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void parsesTsvCoordinatesAndConfidence() {
        String tsv = """
                level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext
                5\t1\t1\t1\t1\t1\t20\t20\t80\t20\t92.5\t이마트
                5\t1\t1\t1\t2\t1\t20\t60\t60\t20\t88.0\t우유
                5\t1\t1\t1\t2\t2\t400\t60\t80\t20\t96.0\t2,900
                """;

        TesseractDocument document = TesseractDocument.parseTsv(4, tsv);

        assertThat(document.lines()).hasSize(2);
        assertThat(document.wordCount()).isEqualTo(3);
        assertThat(document.averageConfidence()).isGreaterThan(90);
        assertThat(document.lines().get(1).text()).isEqualTo("우유 2,900");
    }

    private static TesseractDocument.Word word(String text, int left, double confidence) {
        return new TesseractDocument.Word(text, left, 20, 80, 20, confidence);
    }
}
