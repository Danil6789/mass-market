package com.marketplace.notification.logs;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.notification.logs.dto.EmailLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.marketplace.notification.constant.ApiPath.NOTIFICATIONS_LOGS;

/**
 * Email-log API contract — SpringDoc annotations live here only.
 * Implementation is in {@link EmailLogController}.
 */
@Tag(name = "Notifications", description = "Лог отправленных email-уведомлений (доступ — администратору)")
@RequestMapping(NOTIFICATIONS_LOGS)
public interface EmailLogApi {

    @GetMapping
    @Operation(
            summary = "Список email-уведомлений",
            description = "Пагинированный лог исходящих писем. Доступно только администратору."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Лог получен"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN")
    })
    ResponseEntity<PageResponse<EmailLogResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);
}
