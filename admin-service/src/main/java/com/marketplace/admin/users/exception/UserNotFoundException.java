package com.marketplace.admin.users.exception;

/**
 * Thrown when an admin moderation action references a non-existent user
 * (looked up via the user-service Feign client). Mapped to HTTP 404 by
 * {@link com.marketplace.admin.handler.GlobalExceptionHandler}.
 */
public class UserNotFoundException extends RuntimeException {

    private final Long userId;

    public UserNotFoundException(Long userId) {
        super("User not found: id=" + userId);
        this.userId = userId;
    }

    public static UserNotFoundException forId(Long id) {
        return new UserNotFoundException(id);
    }

    public Long getUserId() {
        return userId;
    }
}
