package com.marketplace.user.favorites.api;

import com.marketplace.user.favorites.dto.AddFavoriteRequest;
import com.marketplace.user.favorites.dto.FavoriteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static com.marketplace.user.constant.ApiPath.FAVORITES_BASE;

/**
 * Favourites API contract.
 *
 * <p>All endpoints require a valid JWT. The product id is supplied either
 * via {@link AddFavoriteRequest} body or as a path variable.</p>
 */
@Tag(name = "Favorites", description = "Избранные товары авторизованного пользователя")
@RequestMapping(FAVORITES_BASE)
public interface FavoriteApi {

    @PostMapping
    @Operation(summary = "Добавить товар в избранное",
            description = "Сохраняет связь user-product в списке избранного текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Товар добавлен в избранное"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    ResponseEntity<FavoriteResponse> add(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestBody @Valid AddFavoriteRequest body);

    @GetMapping
    @Operation(summary = "Список избранного",
            description = "Возвращает все избранные товары текущего пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список получен"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    ResponseEntity<List<FavoriteResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal);

    @DeleteMapping("/{productId}")
    @Operation(summary = "Удалить товар из избранного")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Товар удалён из избранного"),
            @ApiResponse(responseCode = "404", description = "Товар не найден в избранном"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    ResponseEntity<Void> remove(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID товара") @PathVariable("productId") Long productId);
}