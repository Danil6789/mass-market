package com.marketplace.product.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload for partially updating a product. All fields are optional; a
 * {@code null} value means "leave unchanged".
 */
@Data
public class ProductUpdateRequest {

    @Size(max = 200, message = "Название товара должно быть не длиннее 200 символов")
    private String title;

    @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
    private String description;

    @DecimalMin(value = "0.00", inclusive = true, message = "Цена не может быть отрицательной")
    private BigDecimal price;
}