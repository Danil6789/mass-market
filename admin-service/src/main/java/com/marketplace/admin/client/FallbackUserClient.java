package com.marketplace.admin.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback for {@link UserClient}. Returns {@code null} when user-service is
 * unreachable. Admin-service treats {@code null} as "user not found" so a
 * 404 is surfaced to the caller instead of a misleading success.
 */
@Slf4j
@Component
public class FallbackUserClient implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return id -> {
            log.warn("user-service unreachable, falling back to null UserDto for id={}: {}",
                    id, cause.getMessage());
            return null;
        };
    }
}
