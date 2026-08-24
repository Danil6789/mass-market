package com.marketplace.order.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Fallback for {@link ProductClient}. Returns a snapshot with
 * {@code status="UNKNOWN"} when product-service is unreachable so the order
 * service can reject the order explicitly via
 * {@link com.marketplace.order.orders.exception.ProductNotAvailableException}
 * rather than failing with a generic 5xx.
 */
@Slf4j
@Component
public class FallbackProductClient implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return id -> {
            log.warn("product-service unreachable, falling back to UNKNOWN snapshot for id={}: {}",
                    id, cause.getMessage());
            return new ProductSnapshot(id, null, BigDecimal.ZERO, "UNKNOWN");
        };
    }
}
