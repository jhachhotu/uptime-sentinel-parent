package com.sentinel.monitoring_service.service;

import com.sentinel.monitoring_service.dto.CheckoutRequestDTO;
import com.sentinel.monitoring_service.dto.PaymentHistoryDTO;
import com.sentinel.monitoring_service.dto.SubscriptionDetailsDTO;
import com.sentinel.monitoring_service.entity.*;
import com.sentinel.monitoring_service.repository.PaymentTransactionRepository;
import com.sentinel.monitoring_service.repository.UserRepository;
import com.sentinel.monitoring_service.repository.WebsiteRepository;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final UserRepository userRepository;
    private final WebsiteRepository websiteRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    @Value("${FRONTEND_URL:https://client-uptime-frontend.vercel.app}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.trim().isEmpty() && !stripeApiKey.startsWith("your_")) {
            Stripe.apiKey = stripeApiKey.trim();
            log.info("Stripe Java SDK initialized with configured API key.");
        } else {
            log.info("Stripe API key not configured or placeholder used. Payment service operating with Sandbox Simulation fallback.");
        }
    }

    public boolean isStripeLive() {
        return stripeApiKey != null && !stripeApiKey.trim().isEmpty() && !stripeApiKey.startsWith("your_");
    }

    // ─── 1. Get Current User Subscription & Quotas ───
    public SubscriptionDetailsDTO getSubscriptionDetails(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseGet(() -> {
                    // Create minimal user if not present
                    User newUser = User.builder()
                            .email(userEmail)
                            .provider("LOCAL")
                            .subscriptionTier(SubscriptionTier.STARTER)
                            .billingCycle(BillingCycle.MONTHLY)
                            .subscriptionStatus(SubscriptionStatus.ACTIVE)
                            .build();
                    return userRepository.save(newUser);
                });

        SubscriptionTier tier = user.getSubscriptionTier() != null ? user.getSubscriptionTier() : SubscriptionTier.STARTER;
        BillingCycle cycle = user.getBillingCycle() != null ? user.getBillingCycle() : BillingCycle.MONTHLY;
        SubscriptionStatus status = user.getSubscriptionStatus() != null ? user.getSubscriptionStatus() : SubscriptionStatus.ACTIVE;

        long monitorCount = websiteRepository.findByOwnerId(userEmail).size();

        return SubscriptionDetailsDTO.builder()
                .email(user.getEmail())
                .tier(tier)
                .tierDisplayName(tier.getDisplayName())
                .billingCycle(cycle)
                .status(status)
                .maxMonitors(tier.getMaxMonitors())
                .activeMonitorsCount(monitorCount)
                .minIntervalSeconds(tier.getMinIntervalSeconds())
                .monthlyPriceUsd(tier.getMonthlyPriceUsd())
                .annualPriceUsd(tier.getAnnualPriceUsd())
                .currentPeriodEnd(user.getCurrentPeriodEnd())
                .isPaidPlan(tier != SubscriptionTier.STARTER)
                .stripeCustomerId(user.getStripeCustomerId())
                .build();
    }

    // ─── 2. Create Checkout Session (Stripe or Sandbox) ───
    public Map<String, Object> createCheckoutSession(String userEmail, CheckoutRequestDTO request) {
        SubscriptionTier tier = request.getTier();
        BillingCycle cycle = request.getBillingCycle() != null ? request.getBillingCycle() : BillingCycle.MONTHLY;

        if (tier == null || tier == SubscriptionTier.STARTER) {
            throw new IllegalArgumentException("Cannot create a checkout session for the free Starter tier.");
        }

        int priceUsd = tier.getPrice(cycle);
        long priceCents = priceUsd * 100L;

        String successUrl = request.getSuccessUrl();
        if (successUrl == null || successUrl.isEmpty()) {
            successUrl = frontendUrl + "/billing?success=true&tier=" + tier.name() + "&cycle=" + cycle.name();
        }
        String cancelUrl = request.getCancelUrl();
        if (cancelUrl == null || cancelUrl.isEmpty()) {
            cancelUrl = frontendUrl + "/billing?canceled=true";
        }

        Map<String, Object> response = new HashMap<>();

        if (isStripeLive()) {
            try {
                SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setCustomerEmail(userEmail)
                        .setSuccessUrl(successUrl + (successUrl.contains("?") ? "&" : "?") + "session_id={CHECKOUT_SESSION_ID}")
                        .setCancelUrl(cancelUrl)
                        .putMetadata("userEmail", userEmail)
                        .putMetadata("planTier", tier.name())
                        .putMetadata("billingCycle", cycle.name())
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("usd")
                                                        .setUnitAmount(priceCents)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                        .setName("Sentinel " + tier.getDisplayName() + " Plan (" + cycle.name().toLowerCase() + ")")
                                                                        .setDescription("High-performance infrastructure & uptime monitoring - " + tier.getMaxMonitors() + " monitors with " + tier.getMinIntervalSeconds() + "s intervals.")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        );

                Session session = Session.create(paramsBuilder.build());
                response.put("checkoutUrl", session.getUrl());
                response.put("sessionId", session.getId());
                response.put("mode", "stripe");
                return response;
            } catch (Exception e) {
                log.error("Failed to create Stripe checkout session, falling back to sandbox: {}", e.getMessage());
            }
        }

        // Sandbox / Dev simulation response
        String simulatedSessionId = "sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String redirectUrl = successUrl + (successUrl.contains("?") ? "&" : "?")
                + "session_id=" + simulatedSessionId
                + "&simulated=true&tier=" + tier.name() + "&cycle=" + cycle.name();

        response.put("checkoutUrl", redirectUrl);
        response.put("sessionId", simulatedSessionId);
        response.put("mode", "simulation");
        return response;
    }

    // ─── 3. Webhook Handling ───
    @Transactional
    public String handleStripeWebhook(String payload, String sigHeader) {
        Event event;
        try {
            if (stripeWebhookSecret != null && !stripeWebhookSecret.trim().isEmpty() && sigHeader != null) {
                event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret.trim());
            } else {
                event = Event.GSON.fromJson(payload, Event.class);
            }
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Webhook error: " + e.getMessage());
        }

        log.info("Received Stripe webhook event: {}", event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            Event.Data data = event.getData();
            if (data != null && data.getObject() != null) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session != null) {
                    processSuccessfulCheckout(session);
                }
            }
        } else if ("customer.subscription.deleted".equals(event.getType())) {
            handleSubscriptionCancellationWebhook(event);
        }

        return "Webhook processed successfully";
    }

    @Transactional
    public void processSuccessfulCheckout(Session session) {
        Map<String, String> metadata = session.getMetadata();
        String userEmail = metadata != null ? metadata.get("userEmail") : session.getCustomerEmail();
        String planTierStr = metadata != null ? metadata.get("planTier") : "PRO";
        String billingCycleStr = metadata != null ? metadata.get("billingCycle") : "MONTHLY";

        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("Checkout session {} missing user email, cannot apply subscription", session.getId());
            return;
        }

        SubscriptionTier tier = SubscriptionTier.valueOf(planTierStr);
        BillingCycle cycle = BillingCycle.valueOf(billingCycleStr);
        double amount = session.getAmountTotal() != null ? (session.getAmountTotal() / 100.0) : tier.getPrice(cycle);

        applyPlanUpgrade(userEmail, tier, cycle, session.getId(), session.getPaymentIntent(), amount, "PAID", null);
    }

    private void handleSubscriptionCancellationWebhook(Event event) {
        log.info("Handling subscription cancellation webhook");
    }

    // ─── 4. Apply Plan Upgrade (Reused by Webhook & Sandbox Checkout) ───
    @Transactional
    public SubscriptionDetailsDTO applyPlanUpgrade(String userEmail, SubscriptionTier tier, BillingCycle cycle,
                                                  String sessionId, String paymentIntentId, Double amount,
                                                  String status, String receiptUrl) {
        User user = userRepository.findByEmail(userEmail)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(userEmail)
                            .provider("LOCAL")
                            .build();
                    return userRepository.save(newUser);
                });

        user.setSubscriptionTier(tier);
        user.setBillingCycle(cycle);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);

        LocalDateTime periodEnd = (cycle == BillingCycle.ANNUAL)
                ? LocalDateTime.now().plusYears(1)
                : LocalDateTime.now().plusMonths(1);
        user.setCurrentPeriodEnd(periodEnd);

        userRepository.save(user);

        // Record Payment Transaction
        PaymentTransaction tx = PaymentTransaction.builder()
                .userEmail(userEmail)
                .stripeSessionId(sessionId)
                .stripePaymentIntentId(paymentIntentId)
                .amount(amount != null ? amount : (double) tier.getPrice(cycle))
                .currency("USD")
                .planTier(tier)
                .billingCycle(cycle)
                .status(status != null ? status : "PAID")
                .receiptUrl(receiptUrl)
                .description("Sentinel " + tier.getDisplayName() + " Plan (" + cycle.name().toLowerCase() + ")")
                .createdAt(LocalDateTime.now())
                .build();

        paymentTransactionRepository.save(tx);
        log.info("Upgraded user {} to {} ({}) with transaction ID {}", userEmail, tier, cycle, tx.getId());

        return getSubscriptionDetails(userEmail);
    }

    // ─── 5. Cancel Subscription ───
    @Transactional
    public SubscriptionDetailsDTO cancelSubscription(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        user.setSubscriptionTier(SubscriptionTier.STARTER);
        user.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        userRepository.save(user);

        log.info("User {} cancelled subscription, downgraded to STARTER", userEmail);
        return getSubscriptionDetails(userEmail);
    }

    // ─── 6. Payment Transaction History ───
    public List<PaymentHistoryDTO> getPaymentHistory(String userEmail) {
        List<PaymentTransaction> transactions = paymentTransactionRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
        return transactions.stream().map(tx -> PaymentHistoryDTO.builder()
                .id(tx.getId())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .planTier(tx.getPlanTier())
                .billingCycle(tx.getBillingCycle())
                .status(tx.getStatus())
                .receiptUrl(tx.getReceiptUrl())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    // ─── 7. Direct Sandbox Simulation (For instant testing / dev) ───
    @Transactional
    public SubscriptionDetailsDTO simulatePlanUpgrade(String userEmail, SubscriptionTier tier, BillingCycle cycle) {
        if (tier == SubscriptionTier.STARTER) {
            return cancelSubscription(userEmail);
        }

        String mockSessionId = "sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String mockPaymentIntent = "pi_sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        double amount = tier.getPrice(cycle);

        return applyPlanUpgrade(userEmail, tier, cycle, mockSessionId, mockPaymentIntent, amount, "PAID", null);
    }
}
