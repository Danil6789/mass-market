package com.marketplace.notification.client;

/**
 * Minimal projection of the user-service {@code UserProfileResponse} —
 * just enough to look up an email address by user id when reacting to
 * order/product events that carry only {@code userId}/{@code sellerId}/
 * {@code buyerId}.
 */
public record UserDto(Long id, String email) {
}
