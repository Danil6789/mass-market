package com.marketplace.admin.client;

import com.marketplace.admin.products.exception.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback for {@link ProductClient}. Converts unavailability into a
 * {@link ProductNotFoundException} so the admin endpoint surfaces a 404
 * rather than a generic 5xx — the product does not exist from the caller's
 * perspective if product-service cannot confirm it.
 */
@Slf4j
@Component
public class FallbackProductClient implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return id -> {
            log.warn("product-service unreachable while deleting product id={}: {}",
                    id, cause.getMessage());
            throw ProductNotFoundException.forId(id);
        };
    }
}
