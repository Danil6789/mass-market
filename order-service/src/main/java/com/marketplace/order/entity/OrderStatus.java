package com.marketplace.order.entity;

/**
 * Lifecycle states of an {@link Order}.
 *
 * <ul>
 *   <li>{@link #PENDING} — order created, awaiting payment.</li>
 *   <li>{@link #PAID} — payment processed; saga has shipped the product.</li>
 *   <li>{@link #CANCELLED} — buyer/seller/admin cancelled before payment.</li>
 *   <li>{@link #FAILED} — payment service returned a failure; saga compensates.</li>
 * </ul>
 */
public enum OrderStatus {

    PENDING,
    PAID,
    CANCELLED,
    FAILED
}
