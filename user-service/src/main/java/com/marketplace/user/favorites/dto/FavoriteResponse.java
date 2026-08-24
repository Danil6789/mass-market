package com.marketplace.user.favorites.dto;

import java.time.Instant;

/**
 * Single favourite product entry returned to the client.
 */
public record FavoriteResponse(
        Long productId,
        Instant createdAt
) {
}