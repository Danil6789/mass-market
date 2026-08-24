package com.marketplace.user.auth.dto;

import com.marketplace.user.profile.dto.UserProfileResponse;

/**
 * Authentication response — JWT access + refresh tokens and user profile.
 * Java record (immutable) per marketplace convention for response DTOs.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserProfileResponse user
) {
}