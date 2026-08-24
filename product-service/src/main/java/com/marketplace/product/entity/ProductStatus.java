package com.marketplace.product.entity;

/**
 * Lifecycle states of a {@link Product}.
 *
 * <ul>
 *   <li>{@link #ACTIVE} — visible in listings, available to buy.</li>
 *   <li>{@link #RESERVED} — at least one open order exists, awaiting payment.</li>
 *   <li>{@link #SOLD} — paid; no longer available.</li>
 *   <li>{@link #DELETED} — soft-deleted by owner or admin; hidden from listings.</li>
 * </ul>
 */
public enum ProductStatus {

    ACTIVE,
    RESERVED,
    SOLD,
    DELETED
}