package com.marketplace.user.favorites.exception;

/**
 * Thrown when deleting a favourite that does not exist for the current user.
 * Mapped to HTTP 404.
 */
public class FavoriteNotFoundException extends RuntimeException {

    public FavoriteNotFoundException(String message) {
        super(message);
    }
}