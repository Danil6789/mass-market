package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Published when an order is cancelled (by buyer, seller, or admin).
 *
 * <p>Consumed by product-service to restore the product to ACTIVE, and by
 * notification-service to inform the parties involved.</p>
 *
 * <p><b>IMMUTABLE contract</b>.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderCancelledEvent(
        Long orderId,
        Long buyerId,
        Long sellerId,
        Long productId,
        Long cancelledBy,
        String reason,
        Instant timestamp
) {
}