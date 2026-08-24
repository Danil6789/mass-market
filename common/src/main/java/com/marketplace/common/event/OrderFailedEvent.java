package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Published when payment processing for an order fails (mock 5% failure rate).
 *
 * <p>Consumed by product-service as the saga compensation trigger: products
 * reserved for the failed order are restored to ACTIVE.</p>
 *
 * <p><b>IMMUTABLE contract</b>.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderFailedEvent(
        Long orderId,
        Long buyerId,
        String reason,
        Instant timestamp
) {
}