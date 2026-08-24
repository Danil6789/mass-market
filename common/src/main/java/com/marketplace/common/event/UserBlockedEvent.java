package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Published when an admin blocks a user.
 *
 * <p>Consumed by user-service to deactivate the account locally.</p>
 *
 * <p><b>IMMUTABLE contract</b>.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserBlockedEvent(
        Long userId,
        Long blockedBy,
        String reason,
        Instant timestamp
) {
}