package com.marketplace.order.orders.service;

import com.marketplace.order.config.PaymentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock payment processor used by the order-service saga.
 *
 * <p>The {@code successRate} (from {@link PaymentProperties}) controls the
 * probability that {@link #processPayment} returns {@code true}. The
 * {@code delayMs} simulates the round-trip latency of a real PSP. No DB
 * operations are performed, so the method must NOT be wrapped in
 * {@code @Transactional}.</p>
 *
 * <p>On success a fresh {@code paymentId} (UUID) is generated; on failure
 * no id is produced so the caller can leave the order in FAILED state
 * without polluting the {@code payment_id} column.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentProperties props;

    /**
     * Result of a mock payment attempt.
     *
     * @param success    true if the (mock) PSP approved the charge
     * @param paymentId  the PSP transaction id; null when {@code success} is false
     */
    public record PaymentResult(boolean success, String paymentId) {
    }

    /**
     * Process a mock payment for the given amount.
     *
     * @param orderId order identifier (for logging only)
     * @param amount  amount to charge — must be non-negative
     * @return        result with success flag and (when successful) paymentId
     */
    public PaymentResult processPayment(Long orderId, BigDecimal amount) {
        simulateLatency();

        boolean success = ThreadLocalRandom.current().nextDouble() < props.getSuccessRate();
        if (success) {
            String paymentId = UUID.randomUUID().toString();
            log.debug("Mock payment succeeded: orderId={}, amount={}, paymentId={}",
                    orderId, amount, paymentId);
            return new PaymentResult(true, paymentId);
        } else {
            log.debug("Mock payment failed: orderId={}, amount={}", orderId, amount);
            return new PaymentResult(false, null);
        }
    }

    private void simulateLatency() {
        long delay = props.getDelayMs();
        if (delay <= 0L) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Payment latency simulation interrupted");
        }
    }
}
