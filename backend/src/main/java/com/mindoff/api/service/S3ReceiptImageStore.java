package com.mindoff.api.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

@Component
public class S3ReceiptImageStore {
    private final S3Client s3Client;
    private final String bucket;

    public S3ReceiptImageStore(
            S3Client s3Client,
            @Value("${mindoff.receipt.s3-bucket:}") String bucket
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public String store(String fileName, String contentType, byte[] content) {
        if (bucket.isBlank()) {
            throw new ReceiptOcrException("영수증 저장소가 설정되지 않았습니다.");
        }

        String objectKey = "receipts/" + LocalDate.now() + "/" + UUID.randomUUID() + "-" + safeName(fileName);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType)
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .build(),
                RequestBody.fromBytes(content)
        );
        return objectKey;
    }

    private static String safeName(String fileName) {
        String value = fileName == null ? "receipt" : fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return value.isBlank() ? "receipt" : value;
    }
}
