package com.marketplace.user.profile.dto;

import java.time.Instant;

/**
 * Public-facing user profile response (id, email, name, phone, role, timestamps).
 * Java record — immutable per marketplace convention.
 */
public record UserProfileResponse(
        Long id,
        String email,
        String name,
        String phone,
        String role,
        boolean active,
        boolean blocked,
        Instant createdAt,
        Instant updatedAt
) {
}