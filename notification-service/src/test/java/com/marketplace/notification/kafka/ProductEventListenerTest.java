package com.marketplace.notification.kafka;

import com.marketplace.common.event.ProductDeletedEvent;
import com.marketplace.notification.client.UserClient;
import com.marketplace.notification.client.UserDto;
import com.marketplace.notification.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Unit tests for {@link ProductEventListener}.
 */
@ExtendWith(MockitoExtension.class)
class ProductEventListenerTest {

    @Mock private EmailService emailService;
    @Mock private UserClient userClient;

    @InjectMocks private ProductEventListener listener;

    @Test
    void onProductDeleted_sendsSellerEmail() {
        ProductDeletedEvent event = new ProductDeletedEvent(
                101L, 2L, 99L, "policy violation", Instant.now());
        when(userClient.getUserById(2L)).thenReturn(new UserDto(2L, "seller@example.com"));

        listener.onProductDeleted(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> model = ArgumentCaptor.forClass(Map.class);
        verify(emailService).send(eq("PRODUCT_DELETED"), eq("seller@example.com"), model.capture());
        assertThat(model.getValue()).containsEntry("productId", 101L);
    }

    @Test
    void onProductDeleted_skipsEmailWhenUserServiceDown() {
        ProductDeletedEvent event = new ProductDeletedEvent(
                101L, 2L, 99L, "policy violation", Instant.now());
        when(userClient.getUserById(anyLong())).thenReturn(null);

        listener.onProductDeleted(event);

        verify(emailService, never()).send(anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyMap());
    }
}
