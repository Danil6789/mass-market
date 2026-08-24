package com.marketplace.admin.users.api;

import com.marketplace.admin.users.dto.BlockUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.marketplace.admin.constant.ApiPath.ADMIN_USER_BLOCK;
import static com.marketplace.admin.constant.ApiPath.ADMIN_USER_UNBLOCK;

/**
 * Admin user moderation API contract — SpringDoc annotations live here only.
 * Implementation is in
 * {@link com.marketplace.admin.users.controller.AdminUserController}.
 */
@Tag(name = "Admin · Users", description = "Блокировка и разблокировка пользователей (только администратор)")
public interface AdminUserApi {

    @PostMapping(ADMIN_USER_BLOCK)
    @Operation(
            summary = "Заблокировать пользователя",
            description = "Публикует событие user.blocked. Деактивация аккаунта выполняется асинхронно user-service."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Команда принята"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    ResponseEntity<Void> blockUser(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID пользователя") @PathVariable("id") Long id,
            @RequestBody @Valid BlockUserRequest request);

    @PostMapping(ADMIN_USER_UNBLOCK)
    @Operation(
            summary = "Разблокировать пользователя",
            description = "Публикует событие user.unblocked. Реактивация аккаунта выполняется асинхронно user-service."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Команда принята"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    ResponseEntity<Void> unblockUser(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID пользователя") @PathVariable("id") Long id);
}
