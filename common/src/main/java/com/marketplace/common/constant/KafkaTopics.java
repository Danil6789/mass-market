package com.marketplace.common.constant;

/**
 * Canonical names of Kafka topics used across the marketplace platform.
 *
 * <p>All services MUST reference these constants rather than literal strings
 * so topic names can be evolved in a single place. Topic names are
 * dot-separated lowercase.</p>
 *
 * <p><b>IMMUTABLE contract</b> — renaming a topic requires a coordinated
 * migration across all services; never rename after Phase 2.</p>
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Utility class — no instances.
    }

    // -------------------- User events --------------------
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_BLOCKED = "user.blocked";
    public static final String USER_UNBLOCKED = "user.unblocked";

    // -------------------- Product events --------------------
    public static final String PRODUCT_CREATED = "product.created";
    public static final String PRODUCT_DELETED = "product.deleted";
    public static final String PRODUCT_STATUS_CHANGED = "product.status.changed";

    // -------------------- Order events (saga) --------------------
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_CANCELLED = "order.cancelled";
    public static final String ORDER_FAILED = "order.failed";

    // -------------------- Common --------------------

    /**
     * Suffix appended by Spring Kafka to create the Dead-Letter Topic for any
     * {@code @RetryableTopic} consumer. The DLT topic for, e.g.,
     * {@code order.created} is {@code order.created.DLT}.
     */
    public static final String DLT_SUFFIX = ".DLT";

    /**
     * Default partition count for auto-created topics. Three partitions are
     * enough for a single-broker dev environment while leaving room for future
     * parallelism (key-based routing by entity id keeps order per entity).
     */
    public static final int DEFAULT_PARTITIONS = 3;

    /**
     * Default replication factor for auto-created topics. Single broker in dev
     * — production deployments must override this.
     */
    public static final short DEFAULT_REPLICATION_FACTOR = 1;
}