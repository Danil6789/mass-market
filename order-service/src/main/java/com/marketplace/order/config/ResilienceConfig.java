package com.marketplace.order.config;

import org.springframework.context.annotation.Configuration;

/**
 * Marker class — circuit-breaker instances are configured declaratively in
 * {@code application.yml} under {@code resilience4j.circuitbreaker.instances}.
 *
 * <p>The Spring Boot Resilience4j starter picks up the YAML block and
 * constructs a {@code CircuitBreakerRegistry} bean; the Feign client
 * consumes it automatically when {@code fallbackFactory} is set on the
 * {@code @FeignClient} annotation.</p>
 */
@Configuration
public class ResilienceConfig {
}
