package com.marketplace.order.orders.service;

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
import com.marketplace.order.orders.dto.OrderResponse;
import com.marketplace.order.orders.exception.OrderNotFoundException;
import com.marketplace.order.orders.exception.OrderOperationException;
import com.marketplace.order.orders.exception.PaymentFailedException;
import com.marketplace.order.orders.exception.ProductNotAvailableException;
import com.marketplace.order.repository.OrderHistoryRepository;
import com.marketplace.order.repository.OrderRepository;
import com.marketplace.order.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderService}. All collaborators are mocked; no
 * Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderHistoryRepository orderHistoryRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderHistoryMapper orderHistoryMapper;
    @Mock private ProductClient productClient;
    @Mock private PaymentService paymentService;
    @Mock private OrderEventProducer orderEventProducer;

    @InjectMocks private OrderService orderService;

    private AuthenticatedUser buyer;
    private AuthenticatedUser seller;
    private AuthenticatedUser admin;
    private AuthenticatedUser other;
    private ProductSnapshot activeProduct;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        buyer = new AuthenticatedUser(10L, "buyer@example.com", "USER");
        seller = new AuthenticatedUser(20L, "seller@example.com", "USER");
        admin = new AuthenticatedUser(1L, "admin@example.com", "ADMIN");
        other = new AuthenticatedUser(99L, "other@example.com", "USER");

        activeProduct = new ProductSnapshot(7L, seller.id(),
                new BigDecimal("250.00"), "ACTIVE");

        pendingOrder = Order.builder()
                .id(100L)
                .buyerId(buyer.id())
                .sellerId(seller.id())
                .productId(7L)
                .amount(new BigDecimal("250.00"))
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ------------------------------------------------------------------ createOrder

    @Test
    void createOrder_succeeds_whenProductActive_publishesCreatedEvent() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(7L);
        when(productClient.getProduct(7L)).thenReturn(activeProduct);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            o.setCreatedAt(Instant.now());
            o.setUpdatedAt(Instant.now());
            return o;
        });
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return new OrderResponse(o.getId(), o.getBuyerId(), o.getSellerId(), o.getProductId(),
                    o.getAmount(), o.getStatus(), o.getPaymentId(), o.getPaidAt(),
                    o.getCancelledAt(), o.getCancelledBy(), o.getFailureReason(),
                    o.getCreatedAt(), o.getUpdatedAt());
        });

        OrderResponse response = orderService.createOrder(request, buyer);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(response.sellerId()).isEqualTo(seller.id());

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer).publishCreated(captor.capture());
        OrderCreatedEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(100L);
        assertThat(event.productId()).isEqualTo(7L);
        assertThat(event.sellerId()).isEqualTo(seller.id());
        assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    void createOrder_throwsProductNotAvailable_whenProductInactive() {
        ProductSnapshot inactive = new ProductSnapshot(7L, seller.id(),
                new BigDecimal("250.00"), "RESERVED");
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(7L);
        when(productClient.getProduct(7L)).thenReturn(inactive);

        assertThatThrownBy(() -> orderService.createOrder(request, buyer))
                .isInstanceOf(ProductNotAvailableException.class);

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderEventProducer, never()).publishCreated(any());
    }

    @Test
    void createOrder_throwsProductNotAvailable_whenFeignFallback() {
        // Fallback returns status="UNKNOWN" when product-service is down.
        ProductSnapshot unknown = new ProductSnapshot(7L, null,
                BigDecimal.ZERO, "UNKNOWN");
        CreateOrderRequest request = new CreateOrderRequest();
        request.setProductId(7L);
        when(productClient.getProduct(7L)).thenReturn(unknown);

        assertThatThrownBy(() -> orderService.createOrder(request, buyer))
                .isInstanceOf(ProductNotAvailableException.class);

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderEventProducer, never()).publishCreated(any());
    }

    // ------------------------------------------------------------------ pay

    @Test
    void pay_succeeds_whenPaymentSucceeds_publishesPaidEvent() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
        when(paymentService.processPayment(100L, pendingOrder.getAmount()))
                .thenReturn(new PaymentService.PaymentResult(true, "pay-abc"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return new OrderResponse(o.getId(), o.getBuyerId(), o.getSellerId(), o.getProductId(),
                    o.getAmount(), o.getStatus(), o.getPaymentId(), o.getPaidAt(),
                    o.getCancelledAt(), o.getCancelledBy(), o.getFailureReason(),
                    o.getCreatedAt(), o.getUpdatedAt());
        });

        OrderResponse response = orderService.pay(100L, buyer);

        assertThat(response.status()).isEqualTo(OrderStatus.PAID);
        assertThat(response.paymentId()).isEqualTo("pay-abc");
        assertThat(response.paidAt()).isNotNull();

        ArgumentCaptor<OrderPaidEvent> captor = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(orderEventProducer).publishPaid(captor.capture());
        OrderPaidEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(100L);
        assertThat(event.productId()).isEqualTo(7L);
        assertThat(event.paymentId()).isEqualTo("pay-abc");
        verify(orderEventProducer, never()).publishFailed(any());
    }

    @Test
    void pay_throwsPaymentFailed_andPublishesFailedEvent_whenPaymentServiceReturnsFalse() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
        when(paymentService.processPayment(100L, pendingOrder.getAmount()))
                .thenReturn(new PaymentService.PaymentResult(false, null));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> orderService.pay(100L, buyer))
                .isInstanceOf(PaymentFailedException.class);

        ArgumentCaptor<OrderFailedEvent> captor = ArgumentCaptor.forClass(OrderFailedEvent.class);
        verify(orderEventProducer).publishFailed(captor.capture());
        OrderFailedEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(100L);
        assertThat(event.buyerId()).isEqualTo(buyer.id());
        verify(orderEventProducer, never()).publishPaid(any());
    }

    @Test
    void pay_throwsOrderOperation_whenStatusNotPending() {
        Order paid = Order.builder()
                .id(100L).buyerId(buyer.id()).sellerId(seller.id())
                .productId(7L).amount(new BigDecimal("250.00"))
                .status(OrderStatus.PAID).paidAt(Instant.now())
                .paymentId("pay-abc")
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> orderService.pay(100L, buyer))
                .isInstanceOf(OrderOperationException.class);

        verify(orderEventProducer, never()).publishPaid(any());
        verify(orderEventProducer, never()).publishFailed(any());
    }

    @Test
    void pay_throwsOrderOperation_whenStatusCancelled() {
        Order cancelled = Order.builder()
                .id(100L).buyerId(buyer.id()).sellerId(seller.id())
                .productId(7L).amount(new BigDecimal("250.00"))
                .status(OrderStatus.CANCELLED)
                .cancelledAt(Instant.now()).cancelledBy(buyer.id())
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> orderService.pay(100L, buyer))
                .isInstanceOf(OrderOperationException.class);
    }

    @Test
    void pay_throwsAccessDenied_whenNotBuyer() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.pay(100L, other))
                .isInstanceOf(AccessDeniedException.class);

        verify(orderEventProducer, never()).publishPaid(any());
    }

    // ------------------------------------------------------------------ cancel

    @Test
    void cancel_byBuyer_publishesCancelledEvent() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return new OrderResponse(o.getId(), o.getBuyerId(), o.getSellerId(), o.getProductId(),
                    o.getAmount(), o.getStatus(), o.getPaymentId(), o.getPaidAt(),
                    o.getCancelledAt(), o.getCancelledBy(), o.getFailureReason(),
                    o.getCreatedAt(), o.getUpdatedAt());
        });

        OrderResponse response = orderService.cancel(100L, buyer, "changed mind");

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.cancelledBy()).isEqualTo(buyer.id());
        assertThat(response.failureReason()).isEqualTo("changed mind");

        ArgumentCaptor<OrderCancelledEvent> captor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(orderEventProducer).publishCancelled(captor.capture());
        OrderCancelledEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(100L);
        assertThat(event.cancelledBy()).isEqualTo(buyer.id());
        assertThat(event.productId()).isEqualTo(7L);
    }

    @Test
    void cancel_bySeller_publishesCancelledEvent() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return new OrderResponse(o.getId(), o.getBuyerId(), o.getSellerId(), o.getProductId(),
                    o.getAmount(), o.getStatus(), o.getPaymentId(), o.getPaidAt(),
                    o.getCancelledAt(), o.getCancelledBy(), o.getFailureReason(),
                    o.getCreatedAt(), o.getUpdatedAt());
        });

        OrderResponse response = orderService.cancel(100L, seller, "out of stock");

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.cancelledBy()).isEqualTo(seller.id());
        verify(orderEventProducer).publishCancelled(any());
    }

    @Test
    void cancel_byAdmin_publishesCancelledEvent() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return new OrderResponse(o.getId(), o.getBuyerId(), o.getSellerId(), o.getProductId(),
                    o.getAmount(), o.getStatus(), o.getPaymentId(), o.getPaidAt(),
                    o.getCancelledAt(), o.getCancelledBy(), o.getFailureReason(),
                    o.getCreatedAt(), o.getUpdatedAt());
        });

        OrderResponse response = orderService.cancel(100L, admin, "fraud");

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.cancelledBy()).isEqualTo(admin.id());
        verify(orderEventProducer).publishCancelled(any());
    }

    @Test
    void cancel_throwsAccessDenied_whenNotBuyerSellerOrAdmin() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.cancel(100L, other, "nope"))
                .isInstanceOf(AccessDeniedException.class);

        verify(orderEventProducer, never()).publishCancelled(any());
    }

    @Test
    void cancel_throwsOrderOperation_whenAlreadyPaid() {
        Order paid = Order.builder()
                .id(100L).buyerId(buyer.id()).sellerId(seller.id())
                .productId(7L).amount(new BigDecimal("250.00"))
                .status(OrderStatus.PAID).paidAt(Instant.now())
                .paymentId("pay-abc")
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> orderService.cancel(100L, buyer, "too late"))
                .isInstanceOf(OrderOperationException.class);

        verify(orderEventProducer, never()).publishCancelled(any());
    }

    // ------------------------------------------------------------------ getById

    @Test
    void getById_throwsOrderNotFound_whenMissing() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(404L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // ------------------------------------------------------------------ history

    @Test
    void history_throwsOrderNotFound_whenOrderMissing() {
        when(orderRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.history(404L))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
