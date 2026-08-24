package com.marketplace.product.catalog.api;

import com.marketplace.product.catalog.dto.CategoryResponse;
import com.marketplace.product.catalog.dto.CreateCategoryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.marketplace.product.constant.ApiPath.CATEGORIES_BASE;

/**
 * Category API contract — SpringDoc annotations live here only.
 * Implementation is in {@link com.marketplace.product.catalog.controller.CategoryController}.
 */
@Tag(name = "Categories", description = "Категории товаров (плоский список для MVP)")
@RequestMapping(CATEGORIES_BASE)
public interface CategoryApi {

    @GetMapping
    @Operation(
            summary = "Список категорий",
            description = "Возвращает все категории (плоский список)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список получен")
    })
    ResponseEntity<List<CategoryResponse>> list();

    @PostMapping
    @Operation(
            summary = "Создать категорию",
            description = "Доступно только администратору"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Категория создана"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "409", description = "Категория с таким именем уже существует")
    })
    ResponseEntity<CategoryResponse> create(@RequestBody @Valid CreateCategoryRequest request);
}