package com.sentinel.monitoring_service.controller;

import com.sentinel.monitoring_service.dto.CheckoutRequestDTO;
import com.sentinel.monitoring_service.dto.PaymentHistoryDTO;
import com.sentinel.monitoring_service.dto.SubscriptionDetailsDTO;
import com.sentinel.monitoring_service.entity.BillingCycle;
import com.sentinel.monitoring_service.entity.SubscriptionTier;
import com.sentinel.monitoring_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // ─── 1. Get Current Subscription & Usage ───
    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionDetailsDTO> getSubscription(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(paymentService.getSubscriptionDetails(email));
    }

    // ─── 2. Create Checkout Session ───
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> createCheckout(
            @RequestBody CheckoutRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(paymentService.createCheckoutSession(email, request));
    }

    // ─── 3. Stripe Webhook (Public, verified via signature) ───
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        try {
            String result = paymentService.handleStripeWebhook(payload, sigHeader);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Stripe webhook processing error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }

    // ─── 4. Get Payment & Invoice History ───
    @GetMapping("/history")
    public ResponseEntity<List<PaymentHistoryDTO>> getPaymentHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(paymentService.getPaymentHistory(email));
    }

    // ─── 5. Cancel Subscription ───
    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionDetailsDTO> cancelSubscription(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        return ResponseEntity.ok(paymentService.cancelSubscription(email));
    }

    // ─── 6. Confirm Checkout / Complete Upgrade (Sandbox or Callback) ───
    @PostMapping("/confirm")
    public ResponseEntity<SubscriptionDetailsDTO> confirmCheckout(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        String tierStr = payload.getOrDefault("tier", "PRO");
        String cycleStr = payload.getOrDefault("billingCycle", "MONTHLY");
        String sessionId = payload.get("sessionId");

        SubscriptionTier tier = SubscriptionTier.valueOf(tierStr.toUpperCase());
        BillingCycle cycle = BillingCycle.valueOf(cycleStr.toUpperCase());

        SubscriptionDetailsDTO updated = paymentService.applyPlanUpgrade(
                email, tier, cycle, sessionId, null, (double) tier.getPrice(cycle), "PAID", null);
        return ResponseEntity.ok(updated);
    }

    // ─── 7. Developer / QA Direct Plan Simulation ───
    @PostMapping("/simulate")
    public ResponseEntity<SubscriptionDetailsDTO> simulateUpgrade(
            @RequestBody Map<String, String> payload,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        String tierStr = payload.getOrDefault("tier", "PRO");
        String cycleStr = payload.getOrDefault("billingCycle", "MONTHLY");

        SubscriptionTier tier = SubscriptionTier.valueOf(tierStr.toUpperCase());
        BillingCycle cycle = BillingCycle.valueOf(cycleStr.toUpperCase());

        return ResponseEntity.ok(paymentService.simulatePlanUpgrade(email, tier, cycle));
    }
}
