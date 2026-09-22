package com.sentinel.monitoring_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true) // Can be null for Google Auth users
    private String password;

    @Column(nullable = false)
    private String provider; // "LOCAL" or "GOOGLE"

    @Column(nullable = true)
    private String providerId; // Google subject ID

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false)
    @Builder.Default
    private SubscriptionTier subscriptionTier = SubscriptionTier.STARTER;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false)
    @Builder.Default
    private SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (provider == null) {
            provider = "LOCAL";
        }
        if (subscriptionTier == null) {
            subscriptionTier = SubscriptionTier.STARTER;
        }
        if (billingCycle == null) {
            billingCycle = BillingCycle.MONTHLY;
        }
        if (subscriptionStatus == null) {
            subscriptionStatus = SubscriptionStatus.ACTIVE;
        }
    }
}
