package com.marketplace.product.client;

import com.marketplace.product.client.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for user-service. Falls back to {@link FallbackUserClient}
 * (returns {@code null}) when user-service is down — callers handle the
 * null case explicitly.
 *
 * <p>For Phase 4 (before Eureka is fully wired) the URL is pinned via the
 * {@code app.clients.user-service} property; once Eureka is online the
 * {@code url} can be removed and the client resolves the host via the
 * service registry.</p>
 */
@FeignClient(
        name = "user-service",
        url = "${app.clients.user-service:http://localhost:8081}",
        fallbackFactory = FallbackUserClient.class,
        configuration = FeignAuthConfig.class
)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserDto getUser(@PathVariable("id") Long id);
}