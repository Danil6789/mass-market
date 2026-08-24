package com.marketplace.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Published when an admin unblocks a previously blocked user.
 *
 * <p>Consumed by user-service to reactivate the account locally.</p>
 *
 * <p><b>IMMUTABLE contract</b>.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserUnblockedEvent(
        Long userId,
        Long unblockedBy,
        Instant timestamp
) {
}