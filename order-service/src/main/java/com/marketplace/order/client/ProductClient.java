package com.marketplace.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for product-service. Falls back to
 * {@link FallbackProductClient} (returns a snapshot with status="UNKNOWN")
 * when product-service is down — callers treat UNKNOWN as unavailable.
 *
 * <p>For Phase 5 (before Eureka is fully wired) the URL is pinned via the
 * {@code app.clients.product-service} property; once Eureka is online the
 * {@code url} can be removed and the client resolves the host via the
 * service registry.</p>
 */
@FeignClient(
        name = "product-service",
        url = "${app.clients.product-service:http://localhost:8082}",
        fallbackFactory = FallbackProductClient.class,
        configuration = FeignAuthConfig.class
)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductSnapshot getProduct(@PathVariable("id") Long id);
}
