package com.marketplace.product.catalog.exception;

/**
 * Thrown when a category with the same {@code (parent_id, name)} already
 * exists. Mapped to HTTP 409.
 */
public class CategoryAlreadyExistsException extends RuntimeException {

    public CategoryAlreadyExistsException(String message) {
        super(message);
    }
}