package com.marketplace.user.profile.api;

import com.marketplace.user.profile.dto.UpdateProfileRequest;
import com.marketplace.user.profile.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.marketplace.user.constant.ApiPath.CURRENT_USER_URL;
import static com.marketplace.user.constant.ApiPath.USERS_BASE;

/**
 * Profile API contract. JWT is required for every endpoint — the security
 * filter populates the {@link com.marketplace.user.security.AuthenticatedUser}
 * principal that controllers receive via {@code @AuthenticationPrincipal}.
 */
@Tag(name = "Users", description = "Профиль текущего пользователя и просмотр других пользователей")
@RequestMapping(USERS_BASE)
public interface UserApi {

    @GetMapping(CURRENT_USER_URL)
    @Operation(
            summary = "Получить свой профиль",
            description = "Возвращает данные авторизованного пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Профиль получен"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    ResponseEntity<UserProfileResponse> getCurrent(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal);

    @PatchMapping(CURRENT_USER_URL)
    @Operation(
            summary = "Обновить свой профиль",
            description = "Изменяет имя и телефон авторизованного пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Профиль обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    ResponseEntity<UserProfileResponse> updateCurrent(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestBody @Valid UpdateProfileRequest request);

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить пользователя по id",
            description = "Доступно самому пользователю или администратору"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Нет прав на просмотр"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    ResponseEntity<UserProfileResponse> getById(
            @Parameter(description = "ID пользователя") @PathVariable("id") Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal);
}