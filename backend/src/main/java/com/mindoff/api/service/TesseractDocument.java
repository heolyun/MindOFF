package com.mindoff.api.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

record TesseractDocument(int pageSegmentationMode, List<Line> lines) {
    private static final Pattern AMOUNT = Pattern.compile(".*\\d[0-9,.]{2,}.*");
    private static final Pattern RECEIPT_WORD = Pattern.compile(
            ".*(합계|총액|결제|금액|영수증|매장|사업자|total|amount|receipt).*",
            Pattern.CASE_INSENSITIVE
    );

    static TesseractDocument parseTsv(int pageSegmentationMode, String tsv) {
        Map<LineKey, List<Word>> grouped = new LinkedHashMap<>();
        if (tsv != null) {
            tsv.lines().skip(1).forEach(row -> {
                String[] columns = row.split("\\t", 12);
                if (columns.length < 12 || !"5".equals(columns[0]) || columns[11].isBlank()) return;
                try {
                    double confidence = Double.parseDouble(columns[10]);
                    if (confidence < 0) return;
                    LineKey key = new LineKey(
                            Integer.parseInt(columns[1]),
                            Integer.parseInt(columns[2]),
                            Integer.parseInt(columns[3]),
                            Integer.parseInt(columns[4])
                    );
                    grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new Word(
                            columns[11].trim(),
                            Integer.parseInt(columns[6]),
                            Integer.parseInt(columns[7]),
                            Integer.parseInt(columns[8]),
                            Integer.parseInt(columns[9]),
                            confidence
                    ));
                } catch (NumberFormatException ignored) {
                }
            });
        }

        List<Line> lines = grouped.values().stream()
                .map(words -> words.stream().sorted(Comparator.comparingInt(Word::left)).toList())
                .map(Line::new)
                .sorted(Comparator.comparingInt(Line::top).thenComparingInt(Line::left))
                .toList();
        return new TesseractDocument(pageSegmentationMode, lines);
    }

    static TesseractDocument fromPlainText(String text) {
        List<Line> lines = new ArrayList<>();
        int top = 0;
        if (text != null) {
            for (String rawLine : text.lines().toList()) {
                String normalized = rawLine.replaceAll("\\s+", " ").trim();
                if (normalized.isBlank()) continue;
                List<Word> words = new ArrayList<>();
                int left = 0;
                for (String token : normalized.split(" ")) {
                    int width = Math.max(token.length() * 12, 12);
                    words.add(new Word(token, left, top, width, 20, 100));
                    left += width + 12;
                }
                lines.add(new Line(List.copyOf(words)));
                top += 28;
            }
        }
        return new TesseractDocument(6, List.copyOf(lines));
    }

    double averageConfidence() {
        int characterCount = lines.stream().flatMap(line -> line.words().stream())
                .mapToInt(word -> Math.max(word.text().length(), 1)).sum();
        if (characterCount == 0) return 0;
        double weighted = lines.stream().flatMap(line -> line.words().stream())
                .mapToDouble(word -> word.confidence() * Math.max(word.text().length(), 1)).sum();
        return weighted / characterCount;
    }

    int wordCount() {
        return lines.stream().mapToInt(line -> line.words().size()).sum();
    }

    double qualityScore() {
        long amountLines = lines.stream().map(Line::text).filter(text -> AMOUNT.matcher(text).matches()).count();
        long receiptLines = lines.stream().map(Line::text).filter(text -> RECEIPT_WORD.matcher(text).matches()).count();
        long readableLines = lines.stream().filter(line -> line.averageConfidence() >= 35).count();
        return averageConfidence()
                + Math.min(wordCount(), 100) * 0.08
                + Math.min(amountLines, 12) * 1.5
                + Math.min(receiptLines, 5) * 2.5
                + Math.min(readableLines, 20) * 0.25;
    }

    record Line(List<Word> words) {
        String text() {
            return words.stream().map(Word::text).reduce((left, right) -> left + " " + right).orElse("");
        }

        double averageConfidence() {
            int length = words.stream().mapToInt(word -> Math.max(word.text().length(), 1)).sum();
            if (length == 0) return 0;
            return words.stream()
                    .mapToDouble(word -> word.confidence() * Math.max(word.text().length(), 1))
                    .sum() / length;
        }

        int left() {
            return words.stream().mapToInt(Word::left).min().orElse(0);
        }

        int top() {
            return words.stream().mapToInt(Word::top).min().orElse(0);
        }

        int right() {
            return words.stream().mapToInt(word -> word.left() + word.width()).max().orElse(0);
        }
    }

    record Word(String text, int left, int top, int width, int height, double confidence) {
    }

    private record LineKey(int page, int block, int paragraph, int line) {
    }
}
