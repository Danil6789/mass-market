package com.marketplace.product.exception;

/**
 * Generic forbidden exception used when ownership / role checks fail.
 * Mapped to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}