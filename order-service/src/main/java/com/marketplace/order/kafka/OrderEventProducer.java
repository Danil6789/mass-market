package com.marketplace.order.kafka;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.OrderCancelledEvent;
import com.marketplace.common.event.OrderCreatedEvent;
import com.marketplace.common.event.OrderFailedEvent;
import com.marketplace.common.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes order-related events to Kafka. Uses the order id as the partition
 * key so all events for the same order land on the same partition
 * (preserves per-order ordering).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<?> publishCreated(OrderCreatedEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.ORDER_CREATED, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCreatedEvent: orderId={}", event.orderId(), ex);
                    } else {
                        log.debug("Published OrderCreatedEvent: orderId={}, partition={}, offset={}",
                                event.orderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public CompletableFuture<?> publishPaid(OrderPaidEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.ORDER_PAID, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderPaidEvent: orderId={}", event.orderId(), ex);
                    } else {
                        log.debug("Published OrderPaidEvent: orderId={}, partition={}, offset={}",
                                event.orderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public CompletableFuture<?> publishCancelled(OrderCancelledEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.ORDER_CANCELLED, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderCancelledEvent: orderId={}", event.orderId(), ex);
                    } else {
                        log.debug("Published OrderCancelledEvent: orderId={}, partition={}, offset={}",
                                event.orderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public CompletableFuture<?> publishFailed(OrderFailedEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.ORDER_FAILED, String.valueOf(event.orderId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OrderFailedEvent: orderId={}", event.orderId(), ex);
                    } else {
                        log.debug("Published OrderFailedEvent: orderId={}, partition={}, offset={}",
                                event.orderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
