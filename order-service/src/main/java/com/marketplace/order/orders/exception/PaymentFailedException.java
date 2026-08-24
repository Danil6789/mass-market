package com.marketplace.order.orders.exception;

/**
 * Thrown when the mock payment service returns a failure. Mapped to HTTP 402.
 *
 * <p>The order is left in FAILED status so the saga can compensate the
 * product-service reservation back to ACTIVE.</p>
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }
}
