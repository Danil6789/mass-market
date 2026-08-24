package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published when a seller creates a new product listing.
 *
 * <p>Consumed by notification-service (analytics, search-index stub) and other
 * services that need to react to new catalogue entries.</p>
 *
 * <p><b>IMMUTABLE contract</b>.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCreatedEvent(
        Long productId,
        Long sellerId,
        String title,
        BigDecimal price,
        Long categoryId,
        Instant createdAt
) {
}