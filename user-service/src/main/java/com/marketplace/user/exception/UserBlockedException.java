package com.marketplace.user.exception;

/**
 * Thrown when authentication/operation is attempted on a blocked account.
 * Mapped to HTTP 403.
 */
public class UserBlockedException extends RuntimeException {

    public UserBlockedException(String message) {
        super(message);
    }
}