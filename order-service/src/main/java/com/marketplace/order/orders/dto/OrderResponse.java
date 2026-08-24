package com.marketplace.order.orders.dto;

import com.marketplace.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Full order payload returned to clients. Java record (immutable) per
 * marketplace convention for response DTOs.
 */
public record OrderResponse(
        Long id,
        Long buyerId,
        Long sellerId,
        Long productId,
        BigDecimal amount,
        OrderStatus status,
        String paymentId,
        Instant paidAt,
        Instant cancelledAt,
        Long cancelledBy,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
