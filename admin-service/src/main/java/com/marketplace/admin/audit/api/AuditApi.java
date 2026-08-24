package com.marketplace.admin.audit.api;

import com.marketplace.admin.audit.dto.AuditLogResponse;
import com.marketplace.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.marketplace.admin.constant.ApiPath.ADMIN_AUDIT;

/**
 * Audit log API contract — SpringDoc annotations live here only.
 * Implementation is in {@link com.marketplace.admin.audit.controller.AuditController}.
 */
@Tag(name = "Audit", description = "Журнал аудита административных действий и системных событий")
@RequestMapping(ADMIN_AUDIT)
public interface AuditApi {

    @GetMapping
    @Operation(
            summary = "Список записей аудита",
            description = "Пагинированный список (новые сверху). Доступно только администратору."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Лог получен"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
            @ApiResponse(responseCode = "403", description = "Требуется роль ADMIN")
    })
    ResponseEntity<PageResponse<AuditLogResponse>> list(Pageable pageable);
}
