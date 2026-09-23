package com.sentinel.monitoring_service.dto;

import com.sentinel.monitoring_service.entity.BillingCycle;
import com.sentinel.monitoring_service.entity.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryDTO {
    private Long id;
    private Double amount;
    private String currency;
    private SubscriptionTier planTier;
    private BillingCycle billingCycle;
    private String status;
    private String receiptUrl;
    private String description;
    private LocalDateTime createdAt;
}
