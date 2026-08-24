package com.marketplace.product.catalog.dto;

import java.time.Instant;

/**
 * Public-facing category payload. Java record per marketplace convention
 * for response DTOs.
 */
public record CategoryResponse(
        Long id,
        String name,
        Long parentId,
        Instant createdAt
) {
}