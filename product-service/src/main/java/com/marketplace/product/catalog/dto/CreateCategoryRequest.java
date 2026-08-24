package com.marketplace.product.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for creating a new category. Admin-only.
 */
@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Название категории обязательно")
    @Size(min = 1, max = 100, message = "Название категории должно быть от 1 до 100 символов")
    private String name;

    /**
     * Parent category id; {@code null} for a top-level category.
     */
    @Positive(message = "ID родительской категории должен быть положительным")
    private Long parentId;
}