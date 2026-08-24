package com.marketplace.product.products.exception;

/**
 * Thrown when a Kafka consumer attempts an invalid state transition
 * (e.g., ACTIVE → ACTIVE on a non-existent order). Mapped to HTTP 500.
 * Inside a {@code @RetryableTopic} this triggers retry → DLT.
 */
public class IllegalProductStatusTransitionException extends RuntimeException {

    public IllegalProductStatusTransitionException(String message) {
        super(message);
    }
}