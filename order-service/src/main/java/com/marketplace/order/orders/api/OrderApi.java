package com.marketplace.order.orders.api;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.order.orders.dto.CreateOrderRequest;
import com.marketplace.order.orders.dto.OrderHistoryResponse;
import com.marketplace.order.orders.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.marketplace.order.constant.ApiPath.ORDERS_BASE;

/**
 * Order API contract — SpringDoc annotations live here only.
 * Implementation is in {@link com.marketplace.order.orders.controller.OrderController}.
 */
@Tag(name = "Orders", description = "Жизненный цикл заказов и сага через Kafka")
@RequestMapping(ORDERS_BASE)
public interface OrderApi {

    @PostMapping
    @Operation(
            summary = "Создать заказ",
            description = "Создаёт PENDING заказ и публикует OrderCreatedEvent в Kafka. " +
                    "buyerId берётся из JWT principal."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Заказ создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или товар недоступен"),
            @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    ResponseEntity<OrderResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @RequestBody @Valid CreateOrderRequest request);

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить заказ по id",
            description = "Доступно покупателю, продавцу или администратору."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ найден"),
            @ApiResponse(responseCode = "403", description = "Нет прав на просмотр"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    ResponseEntity<OrderResponse> getById(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID заказа") @PathVariable("id") Long id);

    @GetMapping
    @Operation(
            summary = "Список заказов пользователя",
            description = "Возвращает пагинированный список заказов, где caller — buyer или seller."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список получен")
    })
    ResponseEntity<PageResponse<OrderResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "Роль caller: buyer или seller")
                @RequestParam(required = false, defaultValue = "buyer") String role,
            @Parameter(description = "Параметры пагинации (page, size, sort)")
                Pageable pageable);

    @PostMapping("/{id}/pay")
    @Operation(
            summary = "Оплатить заказ",
            description = "Доступно покупателю заказа. Запускает mock-оплату и публикует OrderPaidEvent или OrderFailedEvent."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ оплачен"),
            @ApiResponse(responseCode = "402", description = "Оплата не прошла"),
            @ApiResponse(responseCode = "409", description = "Заказ в неподходящем статусе"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    ResponseEntity<OrderResponse> pay(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID заказа") @PathVariable("id") Long id);

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Отменить заказ",
            description = "Доступно покупателю, продавцу или администратору. Только для PENDING."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Заказ отменён"),
            @ApiResponse(responseCode = "403", description = "Нет прав на отмену"),
            @ApiResponse(responseCode = "409", description = "Заказ в неподходящем статусе"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    ResponseEntity<OrderResponse> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal Object principal,
            @Parameter(description = "ID заказа") @PathVariable("id") Long id,
            @Parameter(description = "Причина отмены (опционально)")
                @RequestBody(required = false) String reason);

    @GetMapping("/{id}/history")
    @Operation(
            summary = "История переходов заказа",
            description = "Возвращает audit trail для заказа."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "История получена"),
            @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    ResponseEntity<List<OrderHistoryResponse>> history(
            @Parameter(description = "ID заказа") @PathVariable("id") Long id);
}
