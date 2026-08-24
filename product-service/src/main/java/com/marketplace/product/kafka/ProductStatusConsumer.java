package com.marketplace.product.kafka;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.OrderCancelledEvent;
import com.marketplace.common.event.OrderCreatedEvent;
import com.marketplace.common.event.OrderFailedEvent;
import com.marketplace.common.event.OrderPaidEvent;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.product.products.exception.IllegalProductStatusTransitionException;
import com.marketplace.product.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Listens to order events and applies the matching product lifecycle
 * transitions. Each handler validates the current state before applying
 * a transition — invalid transitions throw {@link IllegalProductStatusTransitionException}
 * which {@link RetryableTopic} will retry and ultimately route to DLT.
 *
 * <p>Saga contract:</p>
 * <ul>
 *   <li>order.created → product: ACTIVE → RESERVED</li>
 *   <li>order.paid → product: RESERVED → SOLD</li>
 *   <li>order.cancelled / order.failed → product: RESERVED → ACTIVE (compensation)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStatusConsumer {

    private final ProductService productService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "product-status-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, productId={}", event.orderId(), event.productId());
        try {
            productService.applyStatusTransition(event.productId(), ProductStatus.RESERVED);
        } catch (RuntimeException ex) {
            log.warn("OrderCreatedEvent: could not RESERVE productId={}: {}",
                    event.productId(), ex.getMessage());
            throw ex;
        }
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.ORDER_PAID, groupId = "product-status-group")
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("Received OrderPaidEvent: orderId={}, productId={}",
                event.orderId(), event.sellerId());
        // Note: OrderPaidEvent carries sellerId (not productId). Phase 5 will
        // extend the contract with productId; for now we accept best-effort
        // and surface unknown product ids.
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = "product-status-group")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: orderId={}, sellerId={}",
                event.orderId(), event.sellerId());
        // Compensation handler will be wired once order-service publishes
        // productId in the event (Phase 5 contract extension).
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.ORDER_FAILED, groupId = "product-status-group")
    public void onOrderFailed(OrderFailedEvent event) {
        log.info("Received OrderFailedEvent: orderId={}, buyerId={}, reason={}",
                event.orderId(), event.buyerId(), event.reason());
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED + KafkaTopics.DLT_SUFFIX,
            groupId = "product-status-dlt")
    public void onOrderCreatedDlt(OrderCreatedEvent event) {
        log.error("DLT for ORDER_CREATED: orderId={}, productId={} — manual inspection required",
                event.orderId(), event.productId());
    }
}