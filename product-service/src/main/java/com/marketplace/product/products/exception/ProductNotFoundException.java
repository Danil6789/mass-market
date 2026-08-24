package com.marketplace.product.products.exception;

/**
 * Thrown when a product lookup fails. Mapped to HTTP 404.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }
}