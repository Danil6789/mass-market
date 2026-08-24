package com.marketplace.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the order-service. Enables JPA auditing so the
 * {@code @CreatedDate}/{@code @LastModifiedDate} fields on entities populate
 * automatically, and OpenFeign for sync calls to product-service.
 *
 * <p>This service is the producer of order-related Kafka events that drive
 * the marketplace saga (see {@link com.marketplace.order.kafka.OrderEventProducer}).</p>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients
@ConfigurationPropertiesScan
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
