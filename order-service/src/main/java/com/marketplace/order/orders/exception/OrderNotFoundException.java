package com.marketplace.order.orders.exception;

/**
 * Thrown when an order lookup fails. Mapped to HTTP 404.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}
