package com.marketplace.product.client.dto;

import java.time.Instant;

/**
 * Minimal projection of the user-service {@code UserProfileResponse}.
 * Only fields needed by product-service are carried; the rest is dropped
 * to keep the cross-service contract narrow.
 */
public record UserDto(
        Long id,
        String email,
        String name,
        String phone,
        String role,
        Instant createdAt
) {
}