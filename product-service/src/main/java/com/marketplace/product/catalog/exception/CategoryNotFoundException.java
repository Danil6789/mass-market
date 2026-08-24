package com.marketplace.product.catalog.exception;

/**
 * Thrown when a category lookup fails. Mapped to HTTP 404.
 */
public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String message) {
        super(message);
    }
}