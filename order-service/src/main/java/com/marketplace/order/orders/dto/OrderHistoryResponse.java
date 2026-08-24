package com.marketplace.order.orders.dto;

import com.marketplace.order.entity.OrderStatus;

import java.time.Instant;

/**
 * Single entry in an order's audit history.
 */
public record OrderHistoryResponse(
        Long id,
        OrderStatus status,
        Long changedBy,
        String reason,
        Instant changedAt
) {
}
