package com.mindoff.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mindoff.receipt.ocr-mode", havingValue = "tesseract")
public class TesseractReceiptOcrGateway implements ReceiptOcrGateway {
    private final S3ReceiptImageStore imageStore;
    private final TesseractReceiptParser parser;
    private final String language;
    private final long timeoutSeconds;

    public TesseractReceiptOcrGateway(
            S3ReceiptImageStore imageStore,
            TesseractReceiptParser parser,
            @Value("${mindoff.receipt.tesseract-language:kor+eng}") String language,
            @Value("${mindoff.receipt.tesseract-timeout-seconds:30}") long timeoutSeconds
    ) {
        this.imageStore = imageStore;
        this.parser = parser;
        this.language = language;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public OcrDraft analyze(String fileName, String contentType, byte[] content) {
        imageStore.store(fileName, contentType, content);

        Path imageFile = null;
        Path outputFile = null;
        Path errorFile = null;
        Process process = null;
        try {
            imageFile = Files.createTempFile("mindoff-receipt-", suffix(fileName, contentType));
            outputFile = Files.createTempFile("mindoff-ocr-output-", ".txt");
            errorFile = Files.createTempFile("mindoff-ocr-error-", ".txt");
            Files.write(imageFile, content);

            process = new ProcessBuilder(
                    "tesseract",
                    imageFile.toString(),
                    "stdout",
                    "-l", language,
                    "--psm", "6"
            ).redirectOutput(outputFile.toFile())
                    .redirectError(errorFile.toFile())
                    .start();

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ReceiptOcrException("영수증 인식 시간이 초과되었습니다.");
            }
            if (process.exitValue() != 0) {
                throw new ReceiptOcrException("영수증 글자를 인식하지 못했습니다.");
            }
            String text = Files.readString(outputFile, StandardCharsets.UTF_8);
            return parser.parse(text);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ReceiptOcrException("영수증 인식이 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new ReceiptOcrException("영수증 인식기를 실행하지 못했습니다.", exception);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            deleteQuietly(imageFile);
            deleteQuietly(outputFile);
            deleteQuietly(errorFile);
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
}
