package com.marketplace.user.auth.exception;

/**
 * Thrown when the supplied credentials are wrong (email not found or password
 * mismatch) or when a refresh token cannot be validated.
 * Mapped to HTTP 401.
 */
public class BadCredentialsException extends RuntimeException {

    public BadCredentialsException(String message) {
        super(message);
    }
}