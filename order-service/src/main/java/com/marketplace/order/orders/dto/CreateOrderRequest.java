package com.marketplace.order.orders.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Payload for creating a new order. {@code buyerId} is taken from the JWT
 * principal — it is NOT part of the request body.
 */
@Data
public class CreateOrderRequest {

    @NotNull(message = "ID товара обязателен")
    @Positive(message = "ID товара должен быть положительным")
    private Long productId;
}
