package com.marketplace.admin.constant;

/**
 * Categorical discriminator for the entity referenced by an audit row.
 * Stored as a string in {@code audit_log.target_type} via
 * {@code @Enumerated(EnumType.STRING)}.
 */
public enum TargetType {

    USER,
    PRODUCT
}
