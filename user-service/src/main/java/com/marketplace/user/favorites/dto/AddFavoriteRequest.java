package com.marketplace.user.favorites.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Add-to-favourites request body. Wraps the product id with Bean Validation
 * so {@code @Valid @RequestBody} rejects missing/negative ids before they
 * reach the service layer.
 */
@Data
public class AddFavoriteRequest {

    @NotNull(message = "ID товара обязателен")
    @Positive(message = "ID товара должен быть положительным")
    private Long productId;
}