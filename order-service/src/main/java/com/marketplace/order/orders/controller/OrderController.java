package com.marketplace.order.orders.controller;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.order.orders.api.OrderApi;
import com.marketplace.order.orders.dto.CreateOrderRequest;
import com.marketplace.order.orders.dto.OrderHistoryResponse;
import com.marketplace.order.orders.dto.OrderResponse;
import com.marketplace.order.orders.service.OrderService;
import com.marketplace.order.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.marketplace.order.constant.ExceptionMessages.ACCESS_DENIED;

/**
 * REST controller for order endpoints. Delegates to {@link OrderService}
 * and resolves the current user from the security context.
 */
@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @Override
    public ResponseEntity<OrderResponse> create(Object principal, CreateOrderRequest request) {
        AuthenticatedUser user = resolveAuthenticated(principal);
        OrderResponse response = orderService.createOrder(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<OrderResponse> getById(Object principal, Long id) {
        AuthenticatedUser user = resolveAuthenticated(principal);
        return ResponseEntity.ok(orderService.getByIdAuthorized(id, user));
    }

    @Override
    public ResponseEntity<PageResponse<OrderResponse>> list(Object principal, String role, Pageable pageable) {
        AuthenticatedUser user = resolveAuthenticated(principal);
        return ResponseEntity.ok(orderService.list(role, user, pageable));
    }

    @Override
    public ResponseEntity<OrderResponse> pay(Object principal, Long id) {
        AuthenticatedUser user = resolveAuthenticated(principal);
        return ResponseEntity.ok(orderService.pay(id, user));
    }

    @Override
    public ResponseEntity<OrderResponse> cancel(Object principal, Long id, String reason) {
        AuthenticatedUser user = resolveAuthenticated(principal);
        return ResponseEntity.ok(orderService.cancel(id, user, reason));
    }

    @Override
    public ResponseEntity<List<OrderHistoryResponse>> history(Long id) {
        return ResponseEntity.ok(orderService.history(id));
    }

    private static AuthenticatedUser resolveAuthenticated(Object principal) {
        if (!(principal instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        return au;
    }
}
