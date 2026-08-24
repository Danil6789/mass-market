package com.marketplace.user.exception;

/**
 * Thrown when a user lookup fails. Mapped to HTTP 404.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}