package com.marketplace.admin.client;

/**
 * Minimal projection of the user-service user entity. Used by admin-service
 * for existence checks before publishing block / unblock events.
 */
public record UserDto(Long id, String email, Boolean blocked) {
}
