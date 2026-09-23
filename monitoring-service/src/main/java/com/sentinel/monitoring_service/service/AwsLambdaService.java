package com.sentinel.monitoring_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.monitoring_service.config.AwsConfig;
import com.sentinel.monitoring_service.entity.IssueTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.HashMap;
import java.util.Map;

@Service
public class AwsLambdaService {

    private static final Logger log = LoggerFactory.getLogger(AwsLambdaService.class);

    @Autowired
    private AwsConfig awsConfig;

    @Autowired(required = false)
    private LambdaClient lambdaClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class LambdaProcessingResult {
        private final boolean success;
        private final String executionArnOrId;
        private final String message;

        public LambdaProcessingResult(boolean success, String executionArnOrId, String message) {
            this.success = success;
            this.executionArnOrId = executionArnOrId;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getExecutionArnOrId() {
            return executionArnOrId;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Invokes AWS Lambda function to process the issue proof and trigger alerting/validation.
     */
    public LambdaProcessingResult processIssueProof(IssueTicket ticket) {
        if (lambdaClient != null && awsConfig.isAwsConfigured()) {
            String functionName = awsConfig.getLambdaFunctionName();
            try {
                log.info("Invoking AWS Lambda function: {} for Issue ID: {}", functionName, ticket.getId());

                Map<String, Object> payload = new HashMap<>();
                payload.put("eventType", "ISSUE_PROOF_UPLOADED");
                payload.put("issueId", ticket.getId());
                payload.put("title", ticket.getTitle());
                payload.put("description", ticket.getDescription());
                payload.put("category", ticket.getCategory() != null ? ticket.getCategory().name() : "OTHER");
                payload.put("priority", ticket.getPriority() != null ? ticket.getPriority().name() : "MEDIUM");
                payload.put("websiteId", ticket.getWebsiteId());
                payload.put("websiteName", ticket.getWebsiteName());
                payload.put("userEmail", ticket.getUserEmail());
                payload.put("s3Bucket", ticket.getS3Bucket());
                payload.put("s3Key", ticket.getS3Key());
                payload.put("screenshotUrl", ticket.getScreenshotUrl());
                payload.put("timestamp", System.currentTimeMillis());

                String payloadJson = objectMapper.writeValueAsString(payload);

                InvokeRequest invokeRequest = InvokeRequest.builder()
                        .functionName(functionName)
                        .payload(SdkBytes.fromUtf8String(payloadJson))
                        .build();

                InvokeResponse response = lambdaClient.invoke(invokeRequest);
                String responsePayload = response.payload().asUtf8String();
                int statusCode = response.statusCode();

                log.info("AWS Lambda execution completed with status {}: {}", statusCode, responsePayload);

                if (statusCode >= 200 && statusCode < 300) {
                    return new LambdaProcessingResult(true, "AWS_LAMBDA_OK: " + functionName + " (status " + statusCode + ")", responsePayload);
                } else {
                    return new LambdaProcessingResult(false, "AWS_LAMBDA_ERR: " + statusCode, responsePayload);
                }
            } catch (Exception e) {
                log.warn("AWS Lambda execution error for Issue #{}: {}", ticket.getId(), e.getMessage());
                return new LambdaProcessingResult(false, "LAMBDA_ERROR: " + e.getMessage(), e.getMessage());
            }
        } else {
            // Local fallback simulation
            log.info("AWS Lambda not configured. Utilizing local simulated event processor for Issue #{}.", ticket.getId());
            return new LambdaProcessingResult(true, "LOCAL_SIMULATED_LAMBDA", "Processed via local fallback engine");
        }
    }
}
