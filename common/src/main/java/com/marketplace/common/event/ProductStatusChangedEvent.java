package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Published when a product transitions between lifecycle states
 * (ACTIVE → RESERVED → SOLD, or back to ACTIVE on compensation).
 *
 * <p>Consumed by services that need to react to inventory changes (e.g.,
 * notification-service, analytics).</p>
 *
 * <p><b>IMMUTABLE contract</b>.</p>
 *
 * <p>Status values are stored as strings to keep the cross-service contract
 * stable. Use the constants from product-service for canonical names.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductStatusChangedEvent(
        Long productId,
        String oldStatus,
        String newStatus,
        Long orderId
) {
}