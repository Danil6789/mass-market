package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published when a buyer creates a new order.
 *
 * <p>Consumed by product-service to mark the product as RESERVED, and by
 * notification-service to inform the seller.</p>
 *
 * <p><b>IMMUTABLE contract</b>.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderCreatedEvent(
        Long orderId,
        Long buyerId,
        Long sellerId,
        Long productId,
        BigDecimal amount,
        Instant createdAt
) {
}