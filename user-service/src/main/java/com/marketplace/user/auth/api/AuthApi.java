package com.marketplace.user.auth.api;

import com.marketplace.user.auth.dto.AuthResponse;
import com.marketplace.user.auth.dto.LoginRequest;
import com.marketplace.user.auth.dto.RefreshRequest;
import com.marketplace.user.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.marketplace.user.constant.ApiPath.AUTH_BASE;
import static com.marketplace.user.constant.ApiPath.LOGIN_URL;
import static com.marketplace.user.constant.ApiPath.REFRESH_URL;
import static com.marketplace.user.constant.ApiPath.REGISTER_URL;

/**
 * Authentication API contract — SpringDoc annotations live here only.
 * Implementation is in {@link com.marketplace.user.auth.controller.AuthController}.
 */
@Tag(name = "Authentication", description = "Регистрация, вход и обновление JWT-токенов")
@RequestMapping(AUTH_BASE)
public interface AuthApi {

    @PostMapping(REGISTER_URL)
    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создаёт учётную запись и возвращает пару JWT токенов"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Email уже занят")
    })
    ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request);

    @PostMapping(LOGIN_URL)
    @Operation(
            summary = "Вход в систему",
            description = "Проверяет email и пароль, возвращает новые JWT токены"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "401", description = "Неверные учётные данные"),
            @ApiResponse(responseCode = "403", description = "Учётная запись заблокирована")
    })
    ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request);

    @PostMapping(REFRESH_URL)
    @Operation(
            summary = "Обновление токенов",
            description = "Принимает refresh-токен и возвращает новую пару access+refresh"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токены обновлены"),
            @ApiResponse(responseCode = "401", description = "Невалидный refresh-токен")
    })
    ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest request);
}