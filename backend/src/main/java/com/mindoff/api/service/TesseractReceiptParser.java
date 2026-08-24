package com.mindoff.api.service;

import com.mindoff.api.domain.ReceiptItemTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class TesseractReceiptParser {
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2})[./-](\\d{1,2})[./-](\\d{1,2})");
    private static final Pattern TRAILING_AMOUNT = Pattern.compile("^(.*?)([0-9][0-9,.]{1,})(?:\\s*원)?\\s*$");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy/M/d")
    );
    private static final List<String> TOTAL_LABELS = List.of(
            "합계", "총액", "결제금액", "받을금액", "청구금액", "total", "amount due"
    );
    private static final List<String> NON_ITEM_LABELS = List.of(
            "합계", "총액", "소계", "결제", "승인", "카드", "현금", "거스름", "할인", "부가세", "과세", "면세",
            "영수증", "사업자", "대표", "주소", "전화", "tel", "receipt", "total", "amount", "vat", "tax"
    );

    ReceiptOcrGateway.OcrDraft parse(String rawText) {
        List<String> lines = rawText == null ? List.of() : rawText.lines()
                .map(line -> line.replaceAll("\\s+", " ").trim())
                .filter(line -> !line.isBlank())
                .toList();

        LocalDate purchasedAt = findDate(lines);
        String merchantName = findMerchantName(lines);
        List<ReceiptOcrGateway.OcrLine> items = findItems(lines);
        BigDecimal total = findTotal(lines);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            total = items.stream()
                    .map(ReceiptOcrGateway.OcrLine::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return new ReceiptOcrGateway.OcrDraft(merchantName, purchasedAt, total, items);
    }

    private static LocalDate findDate(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = DATE_PATTERN.matcher(line);
            if (!matcher.find()) continue;
            String candidate = matcher.group();
            for (DateTimeFormatter formatter : DATE_FORMATS) {
                try {
                    return LocalDate.parse(candidate, formatter);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return LocalDate.now();
    }

    private static String findMerchantName(List<String> lines) {
        return lines.stream()
                .filter(line -> line.length() >= 2)
                .filter(line -> !DATE_PATTERN.matcher(line).find())
                .filter(line -> !containsAny(line.toLowerCase(Locale.ROOT), NON_ITEM_LABELS))
                .filter(line -> !line.matches(".*\\d{4,}.*"))
                .findFirst()
                .orElse("확인 필요");
    }

    private static List<ReceiptOcrGateway.OcrLine> findItems(List<String> lines) {
        List<ReceiptOcrGateway.OcrLine> items = new ArrayList<>();
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (containsAny(lower, NON_ITEM_LABELS) || DATE_PATTERN.matcher(line).find()) continue;

            Matcher matcher = TRAILING_AMOUNT.matcher(line);
            if (!matcher.matches()) continue;
            BigDecimal amount = parseAmount(matcher.group(2));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) continue;

            String name = matcher.group(1)
                    .replaceFirst("[₩￦]\\s*$", "")
                    .replaceFirst("\\s+[0-9][0-9,.]*(?:\\s+[0-9][0-9,.]*)*$", "")
                    .trim();
            if (name.length() < 2 || name.matches("[0-9 .,:/-]+")) continue;

            items.add(new ReceiptOcrGateway.OcrLine(
                    name,
                    BigDecimal.ONE,
                    amount,
                    amount,
                    guessTarget(name),
                    null
            ));
            if (items.size() == 50) break;
        }
        return List.copyOf(items);
    }

    private static BigDecimal findTotal(List<String> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (String line : lines) {
            if (!containsAny(line.toLowerCase(Locale.ROOT), TOTAL_LABELS)) continue;
            Matcher matcher = TRAILING_AMOUNT.matcher(line);
            if (matcher.matches()) total = parseAmount(matcher.group(2));
        }
        return total;
    }

    private static ReceiptItemTarget guessTarget(String name) {
        String value = name.toLowerCase(Locale.ROOT);
        if (containsAny(value, List.of("우유", "요거트", "치즈", "계란", "두부", "고기", "채소", "과일", "김치", "햄"))) {
            return ReceiptItemTarget.FRIDGE;
        }
        if (containsAny(value, List.of("세제", "휴지", "샴푸", "비누", "치약", "봉투", "수세미", "물티슈"))) {
            return ReceiptItemTarget.HOUSEHOLD_ITEM;
        }
        return ReceiptItemTarget.IGNORE;
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

    private static boolean containsAny(String value, List<String> candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) return true;
        }
        return false;
    }
}
