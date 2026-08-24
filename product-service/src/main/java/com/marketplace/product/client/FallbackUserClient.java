package com.marketplace.product.client;

import com.marketplace.product.client.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback for {@link UserClient}. Returns {@code null} when user-service is
 * unreachable so callers can apply null-safe logic (typically log a warning
 * and trust the JWT principal).
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