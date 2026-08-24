package com.marketplace.notification.constant;

/**
 * Event-type identifiers used as keys in the {@code notification_template}
 * table. Must match the values seeded by
 * {@code V2__init_notification_templates.sql}.
 */
public final class EventTypes {

    private EventTypes() {
        // Utility class — no instances.
    }

    public static final String WELCOME = "WELCOME";
    public static final String NEW_ORDER_SELLER = "NEW_ORDER_SELLER";
    public static final String ORDER_PAID = "ORDER_PAID";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String PRODUCT_DELETED = "PRODUCT_DELETED";

    /** Fallback recipient when user-service is unreachable. */
    public static final String UNKNOWN_EMAIL_SUFFIX = "@unknown";
}
