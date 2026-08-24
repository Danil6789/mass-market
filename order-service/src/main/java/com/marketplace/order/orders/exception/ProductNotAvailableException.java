package com.marketplace.order.orders.exception;

/**
 * Thrown when a referenced product cannot be ordered: it does not exist, is
 * not ACTIVE, or the product-service is unreachable (Feign fallback returned
 * status "UNKNOWN"). Mapped to HTTP 400.
 */
public class ProductNotAvailableException extends RuntimeException {

    public ProductNotAvailableException(String message) {
        super(message);
    }
}
