package com.marketplace.user.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh token request — accepts a previously issued refresh token
 * and returns a new access+refresh pair.
 */
@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token обязателен")
    private String refreshToken;
}