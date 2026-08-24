package com.marketplace.admin.kafka;

import com.marketplace.admin.audit.service.AuditService;
import com.marketplace.admin.constant.AdminActions;
import com.marketplace.admin.entity.AuditLog;
import com.marketplace.common.event.ProductCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ProductAuditConsumer}. Verifies that the consumer:
 * <ul>
 *   <li>appends an {@link AuditLog} row with {@code AUTO_AUDIT_PRODUCT_CREATED} when a product is created,</li>
 *   <li>exposes {@code @RetryableTopic} and {@code @KafkaListener} annotations
 *       so Spring Kafka can wire up the retry + DLT behavior.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ProductAuditConsumerTest {

    @Mock private AuditService auditService;

    @InjectMocks private ProductAuditConsumer consumer;

    @Test
    void onProductCreated_recordsAuditRow() {
        ProductCreatedEvent event = new ProductCreatedEvent(
                100L,
                7L,
                "iPhone 15",
                new BigDecimal("999.00"),
                3L,
                Instant.parse("2024-01-01T00:00:00Z")
        );

        consumer.onProductCreated(event);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).record(captor.capture());
        AuditLog row = captor.getValue();
        assertThat(row.getAdminId()).isEqualTo(0L);
        assertThat(row.getAction()).isEqualTo(AdminActions.AUTO_AUDIT_PRODUCT_CREATED);
        assertThat(row.getTargetId()).isEqualTo(100L);
        assertThat(row.getDetails()).contains("iPhone 15").contains("999.00");
    }

    @Test
    void onProductCreated_isAnnotatedWithRetryableTopicAndKafkaListener() throws NoSuchMethodException {
        Method method = ProductAuditConsumer.class.getMethod("onProductCreated", ProductCreatedEvent.class);
        assertThat(method.isAnnotationPresent(RetryableTopic.class)).isTrue();
        assertThat(method.isAnnotationPresent(KafkaListener.class)).isTrue();

        RetryableTopic retryable = method.getAnnotation(RetryableTopic.class);
        assertThat(retryable.attempts()).isEqualTo("3");
        assertThat(retryable.dltStrategy()).isEqualTo(
                org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR);

        KafkaListener listener = method.getAnnotation(KafkaListener.class);
        assertThat(listener.topics()).containsExactly("product.created");
        assertThat(listener.groupId()).isEqualTo("admin-audit-group");
    }
}
