package com.marketplace.product.kafka;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.ProductCreatedEvent;
import com.marketplace.common.event.ProductDeletedEvent;
import com.marketplace.common.event.ProductStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes product-related events to Kafka. Uses the product id as the
 * partition key so all events for the same product land on the same partition
 * (preserves per-product ordering).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<?> publishCreated(ProductCreatedEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.PRODUCT_CREATED, String.valueOf(event.productId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ProductCreatedEvent: productId={}", event.productId(), ex);
                    } else {
                        log.debug("Published ProductCreatedEvent: productId={}, partition={}, offset={}",
                                event.productId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public CompletableFuture<?> publishDeleted(ProductDeletedEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.PRODUCT_DELETED, String.valueOf(event.productId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ProductDeletedEvent: productId={}", event.productId(), ex);
                    } else {
                        log.debug("Published ProductDeletedEvent: productId={}, partition={}, offset={}",
                                event.productId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public CompletableFuture<?> publishStatusChanged(ProductStatusChangedEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.PRODUCT_STATUS_CHANGED, String.valueOf(event.productId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish ProductStatusChangedEvent: productId={}", event.productId(), ex);
                    } else {
                        log.debug("Published ProductStatusChangedEvent: productId={}, partition={}, offset={}",
                                event.productId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}