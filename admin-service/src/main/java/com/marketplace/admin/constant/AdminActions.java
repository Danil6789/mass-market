package com.marketplace.admin.constant;

/**
 * Action identifiers stored in {@code audit_log.action}. They also appear as
 * Kafka events as part of the admin moderation flow.
 */
public enum AdminActions {

    /** Admin initiated a user block. The actual deactivation is async via Kafka. */
    BLOCK_USER,

    /** Admin initiated a user unblock. */
    UNBLOCK_USER,

    /** Admin removed a product via the product-service Feign client. */
    DELETE_PRODUCT,

    /** Auto-generated audit row appended when admin-service consumes product.created. */
    AUTO_AUDIT_PRODUCT_CREATED
}
