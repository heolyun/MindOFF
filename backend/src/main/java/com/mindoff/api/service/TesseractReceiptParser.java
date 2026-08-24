package com.mindoff.api.service;

import com.mindoff.api.domain.ReceiptItemTarget;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class TesseractReceiptParser {
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2})[./-](\\d{1,2})[./-](\\d{1,2})");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy/M/d")
    );
    private static final List<String> TOTAL_LABELS = List.of(
            "합계", "총액", "결제금액", "받을금액", "청구금액", "총 금액", "total", "amount due"
    );
    private static final List<String> NON_ITEM_LABELS = List.of(
            "합계", "총액", "소계", "결제", "승인", "카드", "현금", "거스름", "할인", "부가세", "과세", "면세",
            "영수증", "사업자", "대표", "주소", "전화", "tel", "receipt", "total", "amount", "vat", "tax",
            "품명", "품목", "수량", "단가", "공급가", "판매금액", "바코드"
    );

    ReceiptOcrGateway.OcrDraft parse(String rawText) {
        return parse(TesseractDocument.fromPlainText(rawText));
    }

    ReceiptOcrGateway.OcrDraft parse(TesseractDocument document) {
        List<TesseractDocument.Line> lines = document.lines().stream()
                .filter(line -> !line.text().isBlank())
                .toList();
        LocalDate purchasedAt = findDate(lines);
        String merchantName = findMerchantName(lines);
        List<ReceiptOcrGateway.OcrLine> items = findItems(lines);
        BigDecimal total = findTotal(lines);
        if (total.compareTo(BigDecimal.ZERO) == 0 && document.averageConfidence() >= 70) {
            total = items.stream()
                    .map(ReceiptOcrGateway.OcrLine::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return new ReceiptOcrGateway.OcrDraft(merchantName, purchasedAt, total, items);
    }

    private static LocalDate findDate(List<TesseractDocument.Line> lines) {
        LocalDate today = LocalDate.now();
        for (TesseractDocument.Line line : lines) {
            Matcher matcher = DATE_PATTERN.matcher(line.text());
            while (matcher.find()) {
                for (DateTimeFormatter formatter : DATE_FORMATS) {
                    try {
                        LocalDate parsed = LocalDate.parse(matcher.group(), formatter);
                        if (!parsed.isBefore(today.minusYears(5)) && !parsed.isAfter(today.plusDays(1))) return parsed;
                    } catch (DateTimeParseException ignored) {
                    }
                }
            }
        }
        return today;
    }

    private static String findMerchantName(List<TesseractDocument.Line> lines) {
        record Candidate(String name, double score) {
        }
        List<Candidate> candidates = new ArrayList<>();
        for (int index = 0; index < Math.min(lines.size(), 10); index++) {
            TesseractDocument.Line line = lines.get(index);
            String text = cleanName(line.text());
            String lower = text.toLowerCase(Locale.ROOT);
            int letters = letterCount(text);
            if (letters < 2 || text.length() > 40) continue;
            if (DATE_PATTERN.matcher(text).find() || containsAny(lower, NON_ITEM_LABELS)) continue;
            if (numericTokenCount(line.words()) > 0) continue;
            double readableRatio = (double) letters / Math.max(text.replace(" ", "").length(), 1);
            if (readableRatio < 0.6 || line.averageConfidence() < 35) continue;
            candidates.add(new Candidate(text, line.averageConfidence() + Math.max(0, 18 - index * 3)));
        }
        return candidates.stream()
                .max(Comparator.comparingDouble(Candidate::score))
                .map(Candidate::name)
                .orElse("확인 필요");
    }

    private static List<ReceiptOcrGateway.OcrLine> findItems(List<TesseractDocument.Line> lines) {
        List<ReceiptOcrGateway.OcrLine> items = new ArrayList<>();
        for (TesseractDocument.Line line : lines) {
            String text = line.text();
            String lower = text.toLowerCase(Locale.ROOT);
            if (line.averageConfidence() < 25 || containsAny(lower, NON_ITEM_LABELS) || DATE_PATTERN.matcher(text).find()) {
                continue;
            }

            List<AmountToken> amounts = amountTokens(line.words());
            AmountToken totalToken = amounts.stream()
                    .filter(token -> token.value().compareTo(BigDecimal.valueOf(500)) >= 0)
                    .max(Comparator.comparing(AmountToken::value))
                    .orElse(null);
            if (totalToken == null) continue;

            int firstNumericIndex = amounts.stream().mapToInt(AmountToken::wordIndex).min().orElse(totalToken.wordIndex());
            String name = cleanName(line.words().subList(0, firstNumericIndex).stream()
                    .map(TesseractDocument.Word::text)
                    .reduce((left, right) -> left + " " + right)
                    .orElse(""));
            if (letterCount(name) < 2 || name.length() > 50 || containsAny(name.toLowerCase(Locale.ROOT), NON_ITEM_LABELS)) {
                continue;
            }

            BigDecimal quantity = findQuantity(amounts, totalToken);
            BigDecimal unitPrice = totalToken.value();
            if (quantity.compareTo(BigDecimal.ONE) > 0) {
                unitPrice = totalToken.value().divide(quantity, 0, RoundingMode.HALF_UP);
            }
            items.add(new ReceiptOcrGateway.OcrLine(
                    name,
                    quantity,
                    unitPrice,
                    totalToken.value(),
                    guessTarget(name),
                    null
            ));
            if (items.size() == 50) break;
        }
        return List.copyOf(items);
    }

    private static BigDecimal findTotal(List<TesseractDocument.Line> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (TesseractDocument.Line line : lines) {
            if (!containsAny(line.text().toLowerCase(Locale.ROOT), TOTAL_LABELS)) continue;
            total = amountTokens(line.words()).stream()
                    .map(AmountToken::value)
                    .max(BigDecimal::compareTo)
                    .orElse(total);
        }
        return total;
    }

    private static BigDecimal findQuantity(List<AmountToken> amounts, AmountToken total) {
        return amounts.stream()
                .filter(token -> token.wordIndex() < total.wordIndex())
                .map(AmountToken::value)
                .filter(value -> value.compareTo(BigDecimal.ONE) >= 0 && value.compareTo(BigDecimal.valueOf(100)) <= 0)
                .filter(value -> total.value().remainder(value).compareTo(BigDecimal.ZERO) == 0)
                .filter(value -> total.value().divide(value, 0, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(500)) >= 0)
                .findFirst()
                .orElse(BigDecimal.ONE);
    }

    private static List<AmountToken> amountTokens(List<TesseractDocument.Word> words) {
        List<AmountToken> result = new ArrayList<>();
        for (int index = 0; index < words.size(); index++) {
            String text = words.get(index).text();
            if (!text.matches(".*\\d.*")) continue;
            String digits = text.replaceAll("[^0-9]", "");
            if (digits.isBlank() || digits.length() > 9) continue;
            try {
                result.add(new AmountToken(new BigDecimal(digits), index));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static int numericTokenCount(List<TesseractDocument.Word> words) {
        return (int) words.stream().filter(word -> word.text().matches(".*\\d.*")).count();
    }

    private static String cleanName(String value) {
        return value.replaceAll("[^\\p{L}0-9()+&/._ -]", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("^[^\\p{L}]+|[^\\p{L}0-9)]+$", "")
                .trim();
    }

    private static int letterCount(String value) {
        return (int) value.codePoints().filter(Character::isLetter).count();
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

    private static boolean containsAny(String value, List<String> candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) return true;
        }
        return false;
    }

    private record AmountToken(BigDecimal value, int wordIndex) {
    }
}
