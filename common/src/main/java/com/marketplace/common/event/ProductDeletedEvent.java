package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Published when a product is removed (either by the seller or by an admin).
 *
 * <p>Consumed by order-service to cancel pending orders referencing this product
 * and by notification-service to inform the seller.</p>
 *
 * <p><b>IMMUTABLE contract</b>.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductDeletedEvent(
        Long productId,
        Long sellerId,
        Long deletedBy,
        String reason,
        Instant timestamp
) {
}