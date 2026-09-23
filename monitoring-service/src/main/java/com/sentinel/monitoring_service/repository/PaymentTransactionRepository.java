package com.sentinel.monitoring_service.repository;

import com.sentinel.monitoring_service.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<PaymentTransaction> findByStripeSessionId(String stripeSessionId);

    Optional<PaymentTransaction> findByStripePaymentIntentId(String stripePaymentIntentId);
}
