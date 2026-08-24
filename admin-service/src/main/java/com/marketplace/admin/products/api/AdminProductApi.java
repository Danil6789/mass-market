package com.marketplace.admin.products.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static com.marketplace.admin.constant.ApiPath.ADMIN_PRODUCT_BY_ID;

/**
 * Admin product moderation API contract — SpringDoc annotations live here only.
 * Implementation is in
 * {@link com.marketplace.admin.products.controller.AdminProductController}.
 */
@Tag(name = "Admin · Products", description = "Удаление товаров администратором")
public interface AdminProductApi {

    @DeleteMapping(ADMIN_PRODUCT_BY_ID)
    @Operation(
            summary = "Удалить товар",
            description = "Администратор инициирует soft-delete товара через product-service. Записывается в audit_log."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Товар удалён"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID товара") @PathVariable("id") Long id);
}
