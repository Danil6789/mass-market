package com.marketplace.admin.kafka;

import com.marketplace.admin.audit.service.AuditService;
import com.marketplace.admin.constant.AdminActions;
import com.marketplace.admin.constant.TargetType;
import com.marketplace.admin.entity.AuditLog;
import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code product.created} and appends an audit row. Retries three
 * times with exponential backoff; on final failure Spring Kafka republishes
 * to {@code product.created.DLT} and the {@link #onProductCreatedDlt} listener
 * records the failure for manual inspection.
 *
 * <p>We deliberately do not trust {@code sellerId} as the admin id — auto-
 * audited events come from the system, not from a human admin, so
 * {@code adminId} is set to {@code 0} as a sentinel value (audit log entries
 * triggered by the system instead of an admin).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductAuditConsumer {

    private static final String GROUP_ID = "admin-audit-group";

    private final AuditService auditService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.PRODUCT_CREATED, groupId = GROUP_ID)
    public void onProductCreated(ProductCreatedEvent event) {
        log.info("Received ProductCreatedEvent: productId={}, title={}", event.productId(), event.title());

        String details = "title=" + event.title() + "; price=" + event.price();
        AuditLog row = AuditLog.builder()
                .adminId(0L)
                .action(AdminActions.AUTO_AUDIT_PRODUCT_CREATED)
                .targetType(TargetType.PRODUCT)
                .targetId(event.productId())
                .details(details)
                .build();
        auditService.record(row);
    }

    @KafkaListener(
            topics = KafkaTopics.PRODUCT_CREATED + KafkaTopics.DLT_SUFFIX,
            groupId = "admin-audit-dlt"
    )
    public void onProductCreatedDlt(ProductCreatedEvent event) {
        log.error("DLT for PRODUCT_CREATED: productId={}, title={} — manual inspection required",
                event.productId(), event.title());
    }
}
