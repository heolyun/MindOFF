package com.mindoff.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mindoff.receipt.ocr-mode", havingValue = "tesseract")
public class TesseractReceiptOcrGateway implements ReceiptOcrGateway {
    private static final Logger log = LoggerFactory.getLogger(TesseractReceiptOcrGateway.class);
    private final S3ReceiptImageStore imageStore;
    private final TesseractReceiptParser parser;
    private final String language;
    private final long timeoutSeconds;

    public TesseractReceiptOcrGateway(
            S3ReceiptImageStore imageStore,
            TesseractReceiptParser parser,
            @Value("${mindoff.receipt.tesseract-language:kor+eng}") String language,
            @Value("${mindoff.receipt.tesseract-timeout-seconds:45}") long timeoutSeconds
    ) {
        this.imageStore = imageStore;
        this.parser = parser;
        this.language = language;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public OcrDraft analyze(String fileName, String contentType, byte[] content) {
        imageStore.store(fileName, contentType, content);

        Path originalFile = null;
        Path preparedFile = null;
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        try {
            originalFile = Files.createTempFile("mindoff-receipt-", suffix(fileName, contentType));
            preparedFile = Files.createTempFile("mindoff-receipt-prepared-", ".png");
            Files.write(originalFile, content);
            boolean preprocessed = preprocess(originalFile, preparedFile, deadlineNanos);

            List<OcrCandidate> candidates = new ArrayList<>();
            addCandidate(candidates, recognize(preprocessed ? preparedFile : originalFile, 4, deadlineNanos), preprocessed);
            addCandidate(candidates, recognize(originalFile, 6, deadlineNanos), false);
            OcrCandidate selectedCandidate = candidates.stream()
                    .max(Comparator.comparingDouble(candidate -> candidate.document().qualityScore()))
                    .orElseThrow(() -> new ReceiptOcrException("영수증 글자를 인식하지 못했습니다."));
            TesseractDocument selected = selectedCandidate.document();
            OcrDraft draft = parser.parse(selected);
            log.info(
                    "Receipt OCR completed: bytes={}, selectedPsm={}, preprocessed={}, confidence={}, words={}, lines={}, items={}, merchantDetected={}, totalDetected={}",
                    content.length,
                    selected.pageSegmentationMode(),
                    selectedCandidate.preprocessed(),
                    Math.round(selected.averageConfidence()),
                    selected.wordCount(),
                    selected.lines().size(),
                    draft.lines().size(),
                    !"확인 필요".equals(draft.merchantName()),
                    draft.totalAmount().signum() > 0
            );
            return draft;
        } catch (IOException exception) {
            throw new ReceiptOcrException("영수증 인식기를 실행하지 못했습니다.", exception);
        } finally {
            deleteQuietly(originalFile);
            deleteQuietly(preparedFile);
        }
    }

    private void addCandidate(List<OcrCandidate> candidates, TesseractDocument document, boolean preprocessed) {
        if (document == null || document.wordCount() == 0) return;
        candidates.add(new OcrCandidate(document, preprocessed));
        log.info("Receipt OCR pass: psm={}, preprocessed={}, confidence={}, words={}, lines={}, quality={}",
                document.pageSegmentationMode(),
                preprocessed,
                Math.round(document.averageConfidence()),
                document.wordCount(),
                document.lines().size(),
                Math.round(document.qualityScore()));
    }

    private boolean preprocess(Path source, Path target, long deadlineNanos) {
        List<String> command = List.of(
                "convert",
                source.toString(),
                "-auto-orient",
                "-background", "white",
                "-alpha", "remove",
                "-alpha", "off",
                "-colorspace", "Gray",
                "-resize", "2400x3200>",
                "-resize", "1600x2200<",
                "-contrast-stretch", "1%x1%",
                "-unsharp", "0x0.75+0.75+0.008",
                "-deskew", "40%",
                target.toString()
        );
        ProcessResult result = run(command, deadlineNanos);
        if (result.success() && fileHasContent(target)) return true;
        log.warn("Receipt image preprocessing failed; using the original image. exitCode={}, timedOut={}",
                result.exitCode(), result.timedOut());
        return false;
    }

    private TesseractDocument recognize(Path input, int psm, long deadlineNanos) {
        List<String> command = List.of(
                "tesseract",
                input.toString(),
                "stdout",
                "-l", language,
                "--oem", "1",
                "--psm", Integer.toString(psm),
                "tsv"
        );
        ProcessResult result = run(command, deadlineNanos);
        if (!result.success()) {
            log.warn("Receipt OCR pass failed. psm={}, exitCode={}, timedOut={}", psm, result.exitCode(), result.timedOut());
            return null;
        }
        return TesseractDocument.parseTsv(psm, result.output());
    }

    private ProcessResult run(List<String> command, long deadlineNanos) {
        Path outputFile = null;
        Path errorFile = null;
        Process process = null;
        try {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) return new ProcessResult(-1, true, "");
            outputFile = Files.createTempFile("mindoff-process-output-", ".txt");
            errorFile = Files.createTempFile("mindoff-process-error-", ".txt");
            process = new ProcessBuilder(command)
                    .redirectOutput(outputFile.toFile())
                    .redirectError(errorFile.toFile())
                    .start();
            if (!process.waitFor(remainingNanos, TimeUnit.NANOSECONDS)) {
                process.destroyForcibly();
                return new ProcessResult(-1, true, "");
            }
            return new ProcessResult(
                    process.exitValue(),
                    false,
                    Files.readString(outputFile, StandardCharsets.UTF_8)
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ReceiptOcrException("영수증 인식이 중단되었습니다.", exception);
        } catch (IOException exception) {
            log.warn("Receipt OCR process could not start: {}", command.getFirst());
            return new ProcessResult(-1, false, "");
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            deleteQuietly(outputFile);
            deleteQuietly(errorFile);
        }
    }

    private static boolean fileHasContent(Path path) {
        try {
            return Files.size(path) > 0;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String suffix(String fileName, String contentType) {
        String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".png") || "image/png".equalsIgnoreCase(contentType)) return ".png";
        if (lowerName.endsWith(".webp") || "image/webp".equalsIgnoreCase(contentType)) return ".webp";
        return ".jpg";
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private record ProcessResult(int exitCode, boolean timedOut, String output) {
        boolean success() {
            return exitCode == 0 && !timedOut;
        }
    }

    private record OcrCandidate(TesseractDocument document, boolean preprocessed) {
    }
}
