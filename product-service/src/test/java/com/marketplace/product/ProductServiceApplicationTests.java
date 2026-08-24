package com.marketplace.product;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that boots the full Spring context for product-service.
 *
 * <p>Disabled by default — the context needs PostgreSQL and Kafka. Run it
 * manually (or in CI) with Docker available.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Docker — run manually in CI")
class ProductServiceApplicationTests {

    @Test
    void contextLoads() {
        // empty — the framework either boots the context or this test fails.
    }
}