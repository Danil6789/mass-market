package com.marketplace.product.products.api;

import com.marketplace.product.products.dto.ProductCreateRequest;
import com.marketplace.product.products.dto.ProductResponse;
import com.marketplace.product.products.dto.ProductSummary;
import com.marketplace.product.products.dto.ProductUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

import static com.marketplace.product.constant.ApiPath.PRODUCTS_BASE;

/**
 * Product API contract — SpringDoc annotations live here only.
 * Implementation is in {@link com.marketplace.product.products.controller.ProductController}.
 */
@Tag(name = "Products", description = "CRUD товаров с фильтрацией и пагинацией")
@RequestMapping(PRODUCTS_BASE)
public interface ProductApi {

    @GetMapping
    @Operation(
            summary = "Список товаров с фильтрами",
            description = "Поддерживает фильтры: categoryId, minPrice, maxPrice, status, search, sellerId. " +
                    "Возвращает пагинированный список."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список получен")
    })
    ResponseEntity<com.marketplace.common.dto.PageResponse<ProductSummary>> list(
            @Parameter(description = "ID категории") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Минимальная цена") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Максимальная цена") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Статус товара") @RequestParam(required = false)
                com.marketplace.product.entity.ProductStatus status,
            @Parameter(description = "Поиск по названию (LIKE, регистр-независимый)")
                @RequestParam(required = false) String search,
            @Parameter(description = "ID продавца") @RequestParam(required = false) Long sellerId,
            @Parameter(description = "Параметры пагинации (page, size, sort)")
                Pageable pageable);

    @GetMapping("/seller/{userId}")
    @Operation(
            summary = "Список товаров продавца",
            description = "Возвращает все товары указанного продавца (без пагинации)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список получен")
    })
    ResponseEntity<List<ProductSummary>> listBySeller(
            @Parameter(description = "ID продавца") @PathVariable("userId") Long userId);

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить товар по id",
            description = "Возвращает полную информацию о товаре"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товар найден"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    ResponseEntity<ProductResponse> getById(
            @Parameter(description = "ID товара") @PathVariable("id") Long id);

    @PostMapping
    @Operation(
            summary = "Создать товар",
            description = "Создаёт новое объявление. sellerId берётся из JWT principal."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Товар создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "404", description = "Категория не найдена")
    })
    ResponseEntity<ProductResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestBody @Valid ProductCreateRequest request);

    @PatchMapping("/{id}")
    @Operation(
            summary = "Обновить товар",
            description = "Частичное обновление. Доступно владельцу или администратору."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Товар обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет прав на изменение"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    ResponseEntity<ProductResponse> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID товара") @PathVariable("id") Long id,
            @RequestBody @Valid ProductUpdateRequest request);

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Удалить товар",
            description = "Soft delete: устанавливает status=DELETED. Доступно владельцу или администратору."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Товар удалён"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет прав на удаление"),
            @ApiResponse(responseCode = "404", description = "Товар не найден")
    })
    ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID товара") @PathVariable("id") Long id);
}