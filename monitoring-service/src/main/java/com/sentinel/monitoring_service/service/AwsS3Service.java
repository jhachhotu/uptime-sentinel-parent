package com.sentinel.monitoring_service.service;

import com.sentinel.monitoring_service.config.AwsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.*;

@Service
public class AwsS3Service {

    private static final Logger log = LoggerFactory.getLogger(AwsS3Service.class);

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/gif"
    );

    @Autowired
    private AwsConfig awsConfig;

    @Autowired(required = false)
    private S3Client s3Client;

    public static class S3UploadResult {
        private final String bucket;
        private final String key;
        private final String fileUrl;
        private final boolean isAwsS3;

        public S3UploadResult(String bucket, String key, String fileUrl, boolean isAwsS3) {
            this.bucket = bucket;
            this.key = key;
            this.fileUrl = fileUrl;
            this.isAwsS3 = isAwsS3;
        }

        public String getBucket() {
            return bucket;
        }

        public String getKey() {
            return key;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public boolean isAwsS3() {
            return isAwsS3;
        }
    }

    /**
     * Uploads an issue proof screenshot to AWS S3, or uses base64 fallback if AWS is not configured.
     */
    public S3UploadResult uploadProof(MultipartFile file, String userEmail) throws IOException {
        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "screenshot.png";
        String cleanFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String safeUser = userEmail.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = "issues/" + safeUser + "/" + UUID.randomUUID() + "-" + cleanFilename;
        String contentType = file.getContentType() != null ? file.getContentType() : "image/png";

        if (s3Client != null && awsConfig.isAwsConfigured()) {
            String bucket = awsConfig.getBucketName();
            log.info("Uploading proof image to AWS S3. Bucket: {}, Key: {}, Size: {} bytes", bucket, key, file.getSize());

            Map<String, String> metadata = new HashMap<>();
            metadata.put("uploaded-by", userEmail);
            metadata.put("upload-time", String.valueOf(System.currentTimeMillis()));

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .metadata(metadata)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucket, awsConfig.getAwsRegion(), key);

            log.info("Successfully uploaded proof to AWS S3: {}", fileUrl);
            return new S3UploadResult(bucket, key, fileUrl, true);
        } else {
            // Local fallback resilience: Encode as Data URI so testing without AWS credentials works seamlessly
            log.info("AWS S3 not configured or credentials missing. Using local resilience fallback (base64 data URI).");
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String dataUri = "data:" + contentType + ";base64," + base64;
            return new S3UploadResult("local-fallback", key, dataUri, false);
        }
    }

    /**
     * Deletes a file from S3 if configured.
     */
    public void deleteFile(String bucket, String key) {
        if (s3Client != null && awsConfig.isAwsConfigured() && bucket != null && key != null && !bucket.equals("local-fallback")) {
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteRequest);
                log.info("Deleted S3 object: {}/{}", bucket, key);
            } catch (Exception e) {
                log.warn("Failed to delete S3 object {}/{}: {}", bucket, key, e.getMessage());
            }
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Proof screenshot file cannot be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Proof file size exceeds maximum limit of 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type: " + contentType + ". Allowed: PNG, JPEG, WEBP, GIF");
        }
    }
}
