package com.marketplace.product.products.dto;

import com.marketplace.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Full product payload returned to clients. Java record (immutable) per
 * marketplace convention for response DTOs.
 */
public record ProductResponse(
        Long id,
        Long sellerId,
        Long categoryId,
        String title,
        String description,
        BigDecimal price,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}