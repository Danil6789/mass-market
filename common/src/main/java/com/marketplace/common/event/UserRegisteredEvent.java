package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Published when a new user successfully registers via user-service.
 *
 * <p>Consumed by notification-service to send a welcome email.</p>
 *
 * <p><b>IMMUTABLE contract</b> — do not modify field names or types after Phase 2.
 * Other services depend on this exact JSON shape.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserRegisteredEvent(
        Long userId,
        String email,
        String name,
        Instant createdAt
) {
}