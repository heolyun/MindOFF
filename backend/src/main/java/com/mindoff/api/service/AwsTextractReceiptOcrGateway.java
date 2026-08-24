package com.mindoff.api.service;

import com.mindoff.api.domain.ReceiptItemTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseRequest;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.ExpenseDocument;
import software.amazon.awssdk.services.textract.model.ExpenseField;
import software.amazon.awssdk.services.textract.model.LineItemFields;
import software.amazon.awssdk.services.textract.model.S3Object;

@Component
@ConditionalOnProperty(name = "mindoff.receipt.ocr-mode", havingValue = "textract")
public class AwsTextractReceiptOcrGateway implements ReceiptOcrGateway {
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    );

    private final S3ReceiptImageStore imageStore;
    private final TextractClient textractClient;
    private final String bucket;

    public AwsTextractReceiptOcrGateway(
            S3ReceiptImageStore imageStore,
            TextractClient textractClient,
            @Value("${mindoff.receipt.s3-bucket}") String bucket
    ) {
        this.imageStore = imageStore;
        this.textractClient = textractClient;
        this.bucket = bucket;
    }

    @Override
    public OcrDraft analyze(String fileName, String contentType, byte[] content) {
        String objectKey = imageStore.store(fileName, contentType, content);

        AnalyzeExpenseResponse response = textractClient.analyzeExpense(AnalyzeExpenseRequest.builder()
                .document(Document.builder()
                        .s3Object(S3Object.builder().bucket(bucket).name(objectKey).build())
                        .build())
                .build());
        if (response.expenseDocuments().isEmpty()) {
            return new OcrDraft("확인 필요", LocalDate.now(), BigDecimal.ZERO, List.of());
        }

        ExpenseDocument document = response.expenseDocuments().getFirst();
        Map<String, String> summary = document.summaryFields().stream()
                .filter(field -> field.type() != null && field.valueDetection() != null)
                .collect(Collectors.toMap(
                        field -> normalized(field.type().text()),
                        field -> field.valueDetection().text(),
                        (first, ignored) -> first
                ));
        List<OcrLine> lines = extractLines(document);
        BigDecimal lineSum = lines.stream().map(OcrLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OcrDraft(
                firstValue(summary, "VENDOR_NAME", "RECEIVER_NAME", "NAME", "확인 필요"),
                parseDate(firstValue(summary, "INVOICE_RECEIPT_DATE", "PURCHASE_DATE", "")),
                parseAmount(firstValue(summary, "TOTAL", "AMOUNT_DUE", lineSum.toPlainString())),
                lines
        );
    }

    private static List<OcrLine> extractLines(ExpenseDocument document) {
        List<OcrLine> result = new ArrayList<>();
        document.lineItemGroups().forEach(group -> group.lineItems().forEach(item -> {
            Map<String, String> fields = item.lineItemExpenseFields().stream()
                    .filter(field -> field.type() != null && field.valueDetection() != null)
                    .collect(Collectors.toMap(
                            field -> normalized(field.type().text()),
                            field -> field.valueDetection().text(),
                            (first, ignored) -> first
                    ));
            String name = firstValue(fields, "ITEM", "PRODUCT_CODE", "EXPENSE_ROW", "품목 확인");
            BigDecimal quantity = parseAmount(firstValue(fields, "QUANTITY", "1"));
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) quantity = BigDecimal.ONE;
            BigDecimal total = parseAmount(firstValue(fields, "PRICE", "TOTAL_PRICE", "AMOUNT", "0"));
            BigDecimal unitPrice = parseAmount(firstValue(fields, "UNIT_PRICE", "0"));
            if (unitPrice.compareTo(BigDecimal.ZERO) == 0 && quantity.compareTo(BigDecimal.ZERO) > 0) {
                unitPrice = total.divide(quantity, 2, java.math.RoundingMode.HALF_UP);
            }
            result.add(new OcrLine(name, quantity, unitPrice, total, guessTarget(name), null));
        }));
        return result;
    }

    private static ReceiptItemTarget guessTarget(String name) {
        String value = name.toLowerCase(Locale.ROOT);
        if (containsAny(value, "우유", "요거트", "치즈", "계란", "두부", "고기", "채소", "과일")) {
            return ReceiptItemTarget.FRIDGE;
        }
        if (containsAny(value, "세제", "휴지", "샴푸", "비누", "치약", "봉투", "수세미")) {
            return ReceiptItemTarget.HOUSEHOLD_ITEM;
        }
        return ReceiptItemTarget.IGNORE;
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) if (value.contains(candidate)) return true;
        return false;
    }

    private static LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return LocalDate.now();
    }

    private static BigDecimal parseAmount(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^0-9.-]", "");
        if (normalized.isBlank() || normalized.equals("-") || normalized.equals(".")) return BigDecimal.ZERO;
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static String firstValue(Map<String, String> values, String... keysAndFallback) {
        for (int index = 0; index < keysAndFallback.length - 1; index++) {
            String value = values.get(keysAndFallback[index]);
            if (value != null && !value.isBlank()) return value;
        }
        return keysAndFallback[keysAndFallback.length - 1];
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
