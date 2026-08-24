package com.marketplace.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Entry point for the notification-service.
 *
 * <p>This service is a Kafka-consumer-only service — it listens to user/order/
 * product events and sends transactional emails via Thymeleaf templates to
 * MailHog. It does not publish any events of its own.</p>
 *
 * <p>Features enabled at the application level:</p>
 * <ul>
 *   <li>{@link EnableJpaAuditing} — populates {@code @CreatedDate} fields automatically.</li>
 *   <li>{@link EnableFeignClients} — sync calls to user-service for email lookup.</li>
 *   <li>{@link EnableMethodSecurity} — enables {@code @PreAuthorize} on controllers.</li>
 *   <li>{@link ConfigurationPropertiesScan} — picks up {@code app.*} property beans.</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@EnableMethodSecurity
@ConfigurationPropertiesScan
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
