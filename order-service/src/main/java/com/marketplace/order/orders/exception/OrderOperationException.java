package com.marketplace.order.orders.exception;

/**
 * Thrown when an operation cannot be performed on the order in its current
 * state (e.g., trying to pay a CANCELLED order, cancelling a PAID order).
 * Mapped to HTTP 409.
 */
public class OrderOperationException extends RuntimeException {

    public OrderOperationException(String message) {
        super(message);
    }
}
