package com.marketplace.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that boots the full Spring context for admin-service.
 *
 * <p>Disabled by default — the context needs PostgreSQL, Kafka, and Eureka.
 * Run it manually (or in CI) with Docker available.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Requires Docker — run manually in CI")
class AdminServiceApplicationTests {

    @Test
    void contextLoads() {
        // empty — the framework either boots the context or this test fails.
    }
}
