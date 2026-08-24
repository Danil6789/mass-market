package com.marketplace.admin.constant;

/**
 * API path constants for the admin-service.
 *
 * <p>All controllers reference these constants via {@code import static}
 * to keep URLs consistent and refactor-safe.</p>
 */
public final class ApiPath {

    private ApiPath() {
        // Utility class — no instances.
    }

    public static final String ADMIN_BASE = "/api/admin";
    public static final String ADMIN_USERS = ADMIN_BASE + "/users";
    public static final String ADMIN_USER_BLOCK = ADMIN_USERS + "/{id}/block";
    public static final String ADMIN_USER_UNBLOCK = ADMIN_USERS + "/{id}/unblock";
    public static final String ADMIN_PRODUCTS = ADMIN_BASE + "/products";
    public static final String ADMIN_PRODUCT_BY_ID = ADMIN_PRODUCTS + "/{id}";
    public static final String ADMIN_AUDIT = ADMIN_BASE + "/audit";
}
