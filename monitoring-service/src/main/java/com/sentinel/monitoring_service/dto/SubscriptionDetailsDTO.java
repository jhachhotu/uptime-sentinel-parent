package com.sentinel.monitoring_service.dto;

import com.sentinel.monitoring_service.entity.BillingCycle;
import com.sentinel.monitoring_service.entity.SubscriptionStatus;
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
public class SubscriptionDetailsDTO {
    private String email;
    private SubscriptionTier tier;
    private String tierDisplayName;
    private BillingCycle billingCycle;
    private SubscriptionStatus status;
    private int maxMonitors;
    private long activeMonitorsCount;
    private int minIntervalSeconds;
    private int monthlyPriceUsd;
    private int annualPriceUsd;
    private LocalDateTime currentPeriodEnd;
    private boolean isPaidPlan;
    private String stripeCustomerId;
}
