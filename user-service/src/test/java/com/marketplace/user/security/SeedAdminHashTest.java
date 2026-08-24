package com.marketplace.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sanity test that ensures the BCrypt hash in {@code V2__seed_admin.sql}
 * actually matches the documented password "Admin123!".
 *
 * <p>If you change the seed migration, update the hash literal below to
 * match and confirm the test still passes.</p>
 */
class SeedAdminHashTest {

    private static final String ADMIN_HASH = "$2a$10$ZVj5t6aAR6jL4BJRu5U2R.vI2mpDzJKo7FtqkdYxNo9FgbJaXNvCa";

    @Test
    void adminHashMatchesAdminPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        assertThat(encoder.matches("Admin123!", ADMIN_HASH))
                .as("Seed admin BCrypt hash must verify against 'Admin123!'")
                .isTrue();
    }
}