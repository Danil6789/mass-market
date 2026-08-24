package com.marketplace.product.products.dto;

import com.marketplace.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Compact product representation used in paginated list endpoints to keep
 * the payload small (no description).
 */
public record ProductSummary(
        Long id,
        Long sellerId,
        Long categoryId,
        String title,
        BigDecimal price,
        ProductStatus status,
        Instant createdAt
) {
}