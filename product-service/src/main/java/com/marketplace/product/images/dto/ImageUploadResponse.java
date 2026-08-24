package com.marketplace.product.images.dto;

import java.time.Instant;

/**
 * Response payload returned after a successful image upload.
 */
public record ImageUploadResponse(
        Long productId,
        Long imageId,
        String url,
        int position,
        Instant createdAt
) {
}