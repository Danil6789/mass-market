package com.marketplace.order.constant;

/**
 * API path constants for the order-service.
 *
 * <p>All controllers reference these constants via {@code import static}
 * to keep URLs consistent and refactor-safe.</p>
 */
public final class ApiPath {

    private ApiPath() {
        // Utility class — no instances.
    }

    public static final String ORDERS_BASE = "/api/orders";
}
