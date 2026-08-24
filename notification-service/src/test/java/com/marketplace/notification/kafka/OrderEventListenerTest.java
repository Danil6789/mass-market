package com.marketplace.notification.kafka;

import com.marketplace.common.event.OrderCancelledEvent;
import com.marketplace.common.event.OrderCreatedEvent;
import com.marketplace.common.event.OrderFailedEvent;
import com.marketplace.common.event.OrderPaidEvent;
import com.marketplace.notification.client.UserClient;
import com.marketplace.notification.client.UserDto;
import com.marketplace.notification.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderEventListener}.
 */
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock private EmailService emailService;
    @Mock private UserClient userClient;

    @InjectMocks private OrderEventListener listener;

    // -------------------------------------------------------------- created

    @Test
    void onOrderCreated_sendsSellerEmail() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                10L, 1L, 2L, 100L, new BigDecimal("199.99"), Instant.now());
        when(userClient.getUserById(2L)).thenReturn(new UserDto(2L, "seller@example.com"));

        listener.onOrderCreated(event);

        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        verify(emailService).send(eq("NEW_ORDER_SELLER"), eq("seller@example.com"), model.capture());
        assertThat(model.getValue()).containsEntry("orderId", 10L);
        assertThat(model.getValue()).containsEntry("amount", new BigDecimal("199.99"));
    }

    @Test
    void onOrderCreated_skipsEmailWhenUserServiceDown() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                10L, 1L, 2L, 100L, new BigDecimal("199.99"), Instant.now());
        when(userClient.getUserById(2L)).thenReturn(null);

        listener.onOrderCreated(event);

        verify(emailService, never()).send(anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyMap());
    }

    // ------------------------------------------------------------------ paid

    @Test
    void onOrderPaid_sendsBuyerEmail() {
        OrderPaidEvent event = new OrderPaidEvent(
                11L, 1L, 2L, 100L, new BigDecimal("199.99"),
                Instant.now(), "payment-1");
        when(userClient.getUserById(1L)).thenReturn(new UserDto(1L, "buyer@example.com"));

        listener.onOrderPaid(event);

        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        verify(emailService).send(eq("ORDER_PAID"), eq("buyer@example.com"), model.capture());
        assertThat(model.getValue()).containsEntry("orderId", 11L);
    }

    @Test
    void onOrderPaid_skipsEmailWhenUserServiceDown() {
        OrderPaidEvent event = new OrderPaidEvent(
                11L, 1L, 2L, 100L, new BigDecimal("199.99"),
                Instant.now(), "payment-1");
        when(userClient.getUserById(anyLong())).thenReturn(null);

        listener.onOrderPaid(event);

        verify(emailService, never()).send(anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyMap());
    }

    // ----------------------------------------------------------- cancelled

    @Test
    void onOrderCancelled_sendsBuyerEmail() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                12L, 1L, 2L, 100L, 1L, "buyer changed mind", Instant.now());
        when(userClient.getUserById(1L)).thenReturn(new UserDto(1L, "buyer@example.com"));

        listener.onOrderCancelled(event);

        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        verify(emailService).send(eq("ORDER_CANCELLED"), eq("buyer@example.com"), model.capture());
        assertThat(model.getValue()).containsEntry("orderId", 12L);
        assertThat(model.getValue()).containsEntry("reason", "buyer changed mind");
    }

    @Test
    void onOrderCancelled_usesDefaultReasonWhenNull() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                12L, 1L, 2L, 100L, 1L, null, Instant.now());
        when(userClient.getUserById(1L)).thenReturn(new UserDto(1L, "buyer@example.com"));

        listener.onOrderCancelled(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        verify(emailService).send(eq("ORDER_CANCELLED"), anyString(), model.capture());
        assertThat(model.getValue()).containsEntry("reason", "не указана");
    }

    // --------------------------------------------------------------- failed

    @Test
    void onOrderFailed_logsWithoutEmail() {
        OrderFailedEvent event = new OrderFailedEvent(
                13L, 1L, "payment declined", Instant.now());

        listener.onOrderFailed(event);

        verify(emailService, never()).send(anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyMap());
    }
}
