package com.marketplace.user.exception;

/**
 * Thrown when attempting to register a user whose email is already taken.
 * Mapped to HTTP 409 in {@link com.marketplace.user.handler.GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}