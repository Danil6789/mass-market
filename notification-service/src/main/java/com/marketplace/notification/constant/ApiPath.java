package com.marketplace.notification.constant;

/**
 * API path constants for the notification-service.
 *
 * <p>All controllers reference these constants via {@code import static}
 * to keep URLs consistent and refactor-safe.</p>
 */
public final class ApiPath {

    private ApiPath() {
        // Utility class — no instances.
    }

    public static final String NOTIFICATIONS_LOGS = "/api/notifications/logs";
}
