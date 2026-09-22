package com.sentinel.monitoring_service.dto;

import com.sentinel.monitoring_service.entity.BillingCycle;
import com.sentinel.monitoring_service.entity.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequestDTO {
    private SubscriptionTier tier;
    private BillingCycle billingCycle;
    private String successUrl;
    private String cancelUrl;
}
