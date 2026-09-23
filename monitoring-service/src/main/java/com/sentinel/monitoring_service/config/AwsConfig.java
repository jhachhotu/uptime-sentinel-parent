package com.sentinel.monitoring_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    private static final Logger log = LoggerFactory.getLogger(AwsConfig.class);

    @Value("${AWS_REGION:us-east-1}")
    private String awsRegion;

    @Value("${AWS_ACCESS_KEY_ID:}")
    private String accessKeyId;

    @Value("${AWS_SECRET_ACCESS_KEY:}")
    private String secretAccessKey;

    @Value("${AWS_S3_BUCKET_NAME:sentinel-issue-proofs}")
    private String bucketName;

    @Value("${AWS_LAMBDA_FUNCTION_NAME:sentinel-issue-processor}")
    private String lambdaFunctionName;

    public boolean isAwsConfigured() {
        return accessKeyId != null && !accessKeyId.trim().isEmpty() &&
               secretAccessKey != null && !secretAccessKey.trim().isEmpty() &&
               !accessKeyId.equalsIgnoreCase("dummy") &&
               !accessKeyId.equalsIgnoreCase("your_aws_access_key");
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getLambdaFunctionName() {
        return lambdaFunctionName;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    @Bean
    public S3Client s3Client() {
        if (!isAwsConfigured()) {
            log.info("AWS credentials not provided. S3Client initialized in local resilience fallback mode.");
            return null;
        }

        try {
            log.info("Initializing AWS S3Client with region: {}", awsRegion);
            return S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    ))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to initialize AWS S3Client (will use local fallback): {}", e.getMessage());
            return null;
        }
    }

    @Bean
    public LambdaClient lambdaClient() {
        if (!isAwsConfigured()) {
            log.info("AWS credentials not provided. LambdaClient initialized in local resilience fallback mode.");
            return null;
        }

        try {
            log.info("Initializing AWS LambdaClient with region: {}", awsRegion);
            return LambdaClient.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    ))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to initialize AWS LambdaClient (will use local fallback): {}", e.getMessage());
            return null;
        }
    }
}
