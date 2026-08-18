package com.mindoff.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.textract.TextractClient;

@Configuration
@ConditionalOnProperty(name = "mindoff.receipt.ocr-mode", havingValue = "textract")
public class AwsClientConfig {
    @Bean
    S3Client s3Client(@Value("${mindoff.aws.region:ap-northeast-2}") String region) {
        return S3Client.builder().region(Region.of(region)).build();
    }

    @Bean
    TextractClient textractClient(@Value("${mindoff.aws.region:ap-northeast-2}") String region) {
        return TextractClient.builder().region(Region.of(region)).build();
    }
}
