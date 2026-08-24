package com.marketplace.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for user-service. The admin moderation flow calls
 * {@code getUserById} to verify that the user exists before publishing
 * block / unblock events.
 *
 * <p>The URL is pinned via {@code app.clients.user-service} so the service
 * can run before Eureka is fully online; once Eureka is wired the {@code url}
 * attribute can be removed and the client resolves the host via the service
 * registry.</p>
 */
@FeignClient(
        name = "user-service",
        url = "${app.clients.user-service:http://localhost:8081}",
        fallbackFactory = FallbackUserClient.class
)
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
