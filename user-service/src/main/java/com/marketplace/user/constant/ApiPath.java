package com.marketplace.user.constant;

/**
 * API path constants for the user-service.
 *
 * <p>All controllers reference these constants via {@code import static}
 * to keep URLs consistent and refactor-safe.</p>
 */
public final class ApiPath {

    private ApiPath() {
        // Utility class — no instances.
    }

    public static final String AUTH_BASE = "/api/auth";
    public static final String REGISTER_URL = "/register";
    public static final String LOGIN_URL = "/login";
    public static final String REFRESH_URL = "/refresh";

    public static final String USERS_BASE = "/api/users";
    public static final String CURRENT_USER_URL = "/me";

    public static final String FAVORITES_BASE = "/api/favorites";
}