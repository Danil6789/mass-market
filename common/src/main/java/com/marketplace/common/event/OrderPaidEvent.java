package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published when an order is successfully paid (mock payment in MVP).
 *
 * <p>Consumed by product-service to transition the product to SOLD, and by
 * notification-service to inform both buyer and seller.</p>
 *
 * <p><b>IMMUTABLE contract</b>.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderPaidEvent(
        Long orderId,
        Long buyerId,
        Long sellerId,
        BigDecimal amount,
        Instant paidAt,
        String paymentId
) {
}