package com.marketplace.product.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload for creating a new product. {@code sellerId} is taken from the
 * JWT principal — it is NOT part of the request body.
 */
@Data
public class ProductCreateRequest {

    @NotNull(message = "ID категории обязателен")
    @Positive(message = "ID категории должен быть положительным")
    private Long categoryId;

    @NotBlank(message = "Название товара обязательно")
    @Size(min = 1, max = 200, message = "Название товара должно быть от 1 до 200 символов")
    private String title;

    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    private String description;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0.00", inclusive = true, message = "Цена не может быть отрицательной")
    private BigDecimal price;
}