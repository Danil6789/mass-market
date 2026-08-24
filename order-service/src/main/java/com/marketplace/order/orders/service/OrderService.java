package com.marketplace.order.orders.service;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.common.event.OrderCancelledEvent;
import com.marketplace.common.event.OrderCreatedEvent;
import com.marketplace.common.event.OrderFailedEvent;
import com.marketplace.common.event.OrderPaidEvent;
import com.marketplace.order.client.ProductClient;
import com.marketplace.order.client.ProductSnapshot;
import com.marketplace.order.entity.Order;
import com.marketplace.order.entity.OrderHistory;
import com.marketplace.order.entity.OrderStatus;
import com.marketplace.order.kafka.OrderEventProducer;
import com.marketplace.order.mapper.OrderHistoryMapper;
import com.marketplace.order.mapper.OrderMapper;
import com.marketplace.order.orders.dto.CreateOrderRequest;
import com.marketplace.order.orders.dto.OrderHistoryResponse;
import com.marketplace.order.orders.dto.OrderResponse;
import com.marketplace.order.orders.exception.OrderNotFoundException;
import com.marketplace.order.orders.exception.OrderOperationException;
import com.marketplace.order.orders.exception.PaymentFailedException;
import com.marketplace.order.orders.exception.ProductNotAvailableException;
import com.marketplace.order.repository.OrderHistoryRepository;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.marketplace.order.constant.ExceptionMessages.ACCESS_DENIED;
import static com.marketplace.order.constant.ExceptionMessages.ORDER_NOT_FOUND;
import static com.marketplace.order.constant.ExceptionMessages.ORDER_OPERATION_FAILED;
import static com.marketplace.order.constant.ExceptionMessages.PAYMENT_FAILED;
import static com.marketplace.order.constant.ExceptionMessages.PRODUCT_NOT_AVAILABLE;

/**
 * Order lifecycle and saga choreography.
 *
 * <p>The marketplace saga is driven by Kafka events:</p>
 * <ul>
 *   <li>{@link #createOrder} → fetches the product snapshot via Feign (with
 *       Resilience4j fallback), persists the order in PENDING, then publishes
 *       {@link OrderCreatedEvent}.</li>
 *   <li>{@link #pay} → runs the mock {@link PaymentService}, sets PAID/FAILED,
 *       then publishes {@link OrderPaidEvent} or {@link OrderFailedEvent}.</li>
 *   <li>{@link #cancel} → sets CANCELLED and publishes
 *       {@link OrderCancelledEvent}.</li>
 * </ul>
 *
 * <p>The product-service consumer reacts to these events to apply the
 * matching lifecycle transitions (RESERVED → SOLD → ACTIVE).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderMapper orderMapper;
    private final OrderHistoryMapper orderHistoryMapper;
    private final ProductClient productClient;
    private final PaymentService paymentService;
    private final OrderEventProducer orderEventProducer;

    // ============================================================
    //                       read side
    // ============================================================

    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByIdAuthorized(Long id, AuthenticatedUser caller) {
        OrderResponse response = getById(id);
        // Buyer, seller of the order, or admin can read it.
        if (caller.isAdmin()) {
            return response;
        }
        if (caller.id().equals(response.buyerId()) || caller.id().equals(response.sellerId())) {
            return response;
        }
        throw new AccessDeniedException(ACCESS_DENIED);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(String role, AuthenticatedUser caller, Pageable pageable) {
        Page<Order> page;
        if ("seller".equalsIgnoreCase(role)) {
            page = orderRepository.findAllBySellerIdOrderByCreatedAtDesc(caller.id(), pageable);
        } else {
            // Default: orders where the caller is the buyer.
            page = orderRepository.findAllByBuyerIdOrderByCreatedAtDesc(caller.id(), pageable);
        }
        List<OrderResponse> content = page.getContent().stream()
                .map(orderMapper::toResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<OrderHistoryResponse> history(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException(ORDER_NOT_FOUND);
        }
        return orderHistoryRepository.findAllByOrderIdOrderByChangedAtAsc(id).stream()
                .map(orderHistoryMapper::toResponse)
                .toList();
    }

    // ============================================================
    //                     saga write side
    // ============================================================

    /**
     * Create an order. The product snapshot is fetched via Feign — if the
     * product is not ACTIVE (or product-service is down, which yields
     * status="UNKNOWN" from the fallback), the order is rejected.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, AuthenticatedUser buyer) {
        Long productId = request.getProductId();

        ProductSnapshot product = productClient.getProduct(productId);
        if (product == null || !"ACTIVE".equalsIgnoreCase(product.status())) {
            log.warn("Order rejected: product unavailable productId={}, snapshotStatus={}",
                    productId, product == null ? "null" : product.status());
            throw new ProductNotAvailableException(PRODUCT_NOT_AVAILABLE);
        }

        Order order = Order.builder()
                .buyerId(buyer.id())
                .sellerId(product.sellerId())
                .productId(productId)
                .amount(product.price())
                .status(OrderStatus.PENDING)
                .build();
        Order saved = orderRepository.save(order);
        log.info("Order created: id={}, buyerId={}, sellerId={}, productId={}, amount={}",
                saved.getId(), saved.getBuyerId(), saved.getSellerId(),
                saved.getProductId(), saved.getAmount());

        appendHistory(saved.getId(), OrderStatus.PENDING, buyer.id(), null);

        Instant occurredAt = saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now();
        orderEventProducer.publishCreated(new OrderCreatedEvent(
                saved.getId(),
                saved.getBuyerId(),
                saved.getSellerId(),
                saved.getProductId(),
                saved.getAmount(),
                occurredAt
        ));

        return orderMapper.toResponse(saved);
    }

    /**
     * Pay a PENDING order. Only the buyer can pay. On payment failure the
     * order is marked FAILED and {@link OrderFailedEvent} is published so
     * product-service can restore the product back to ACTIVE.
     */
    @Transactional
    public OrderResponse pay(Long orderId, AuthenticatedUser caller) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND));

        if (!caller.id().equals(order.getBuyerId()) && !caller.isAdmin()) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderOperationException(ORDER_OPERATION_FAILED);
        }

        PaymentService.PaymentResult result = paymentService.processPayment(order.getId(), order.getAmount());

        if (result.success()) {
            Instant now = Instant.now();
            order.setStatus(OrderStatus.PAID);
            order.setPaymentId(result.paymentId());
            order.setPaidAt(now);
            Order saved = orderRepository.save(order);
            log.info("Order paid: id={}, paymentId={}", saved.getId(), saved.getPaymentId());

            appendHistory(saved.getId(), OrderStatus.PAID, caller.id(), null);

            orderEventProducer.publishPaid(new OrderPaidEvent(
                    saved.getId(),
                    saved.getBuyerId(),
                    saved.getSellerId(),
                    saved.getProductId(),
                    saved.getAmount(),
                    now,
                    saved.getPaymentId()
            ));

            return orderMapper.toResponse(saved);
        } else {
            String reason = "Mock payment failed (rate=" + 0 + ")";
            Instant now = Instant.now();
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason(reason);
            Order saved = orderRepository.save(order);
            log.info("Order payment failed: id={}, reason={}", saved.getId(), reason);

            appendHistory(saved.getId(), OrderStatus.FAILED, caller.id(), reason);

            orderEventProducer.publishFailed(new OrderFailedEvent(
                    saved.getId(),
                    saved.getBuyerId(),
                    reason,
                    now
            ));

            throw new PaymentFailedException(PAYMENT_FAILED);
        }
    }

    /**
     * Cancel a PENDING order. Allowed for buyer, seller or admin. PAID orders
     * are not cancellable in the MVP (no refund flow).
     */
    @Transactional
    public OrderResponse cancel(Long orderId, AuthenticatedUser caller, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NOT_FOUND));

        boolean isBuyer = caller.id().equals(order.getBuyerId());
        boolean isSeller = caller.id().equals(order.getSellerId());
        if (!isBuyer && !isSeller && !caller.isAdmin()) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderOperationException(ORDER_OPERATION_FAILED);
        }

        Instant now = Instant.now();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        order.setCancelledBy(caller.id());
        order.setFailureReason(reason);
        Order saved = orderRepository.save(order);
        log.info("Order cancelled: id={}, by={}, reason={}", saved.getId(), caller.id(), reason);

        appendHistory(saved.getId(), OrderStatus.CANCELLED, caller.id(), reason);

        orderEventProducer.publishCancelled(new OrderCancelledEvent(
                saved.getId(),
                saved.getBuyerId(),
                saved.getSellerId(),
                saved.getProductId(),
                caller.id(),
                reason,
                now
        ));

        return orderMapper.toResponse(saved);
    }

    private void appendHistory(Long orderId, OrderStatus status, Long changedBy, String reason) {
        OrderHistory history = OrderHistory.builder()
                .orderId(orderId)
                .status(status)
                .changedAt(Instant.now())
                .changedBy(changedBy)
                .reason(reason)
                .build();
        orderHistoryRepository.save(history);
    }
}
