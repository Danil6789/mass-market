package com.marketplace.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Entry point for the admin-service.
 *
 * <p>Performs admin-only moderation (block/unblock users, delete products)
 * and persists an audit log of every action. Reacts asynchronously to
 * {@code product.created} events from product-service.</p>
 *
 * <p>Features enabled at the application level:</p>
 * <ul>
 *   <li>{@link EnableJpaAuditing} — populates {@code @CreatedDate} on {@code audit_log}.</li>
 *   <li>{@link EnableFeignClients} — sync calls to user-service and product-service.</li>
 *   <li>{@link EnableMethodSecurity} — enables {@code @PreAuthorize} on controllers.</li>
 *   <li>{@link ConfigurationPropertiesScan} — picks up {@code app.*} property beans.</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableMethodSecurity
@ConfigurationPropertiesScan
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
