package com.marketplace.order.client;

import java.math.BigDecimal;

/**
 * Minimal projection of the product-service product entity. Only the fields
 * the order-service needs are carried: the seller id (so the order knows who
 * to pay), the price (so we can store the amount at order time), and the
 * status (so we can reject the order if the product is not ACTIVE).
 *
 * <p>A status of {@code "UNKNOWN"} is produced by
 * {@link FallbackProductClient} when product-service is unreachable; the
 * service layer treats that case as unavailable.</p>
 */
public record ProductSnapshot(
        Long id,
        Long sellerId,
        BigDecimal price,
        String status
) {
}
