package com.marketplace.notification.kafka;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.OrderCancelledEvent;
import com.marketplace.common.event.OrderCreatedEvent;
import com.marketplace.common.event.OrderFailedEvent;
import com.marketplace.common.event.OrderPaidEvent;
import com.marketplace.notification.client.UserClient;
import com.marketplace.notification.client.UserDto;
import com.marketplace.notification.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.marketplace.notification.constant.EventTypes.NEW_ORDER_SELLER;
import static com.marketplace.notification.constant.EventTypes.ORDER_CANCELLED;
import static com.marketplace.notification.constant.EventTypes.ORDER_PAID;
import static com.marketplace.notification.constant.EventTypes.UNKNOWN_EMAIL_SUFFIX;

/**
 * Listens to order events and dispatches the matching email.
 *
 * <p>Order events carry only {@code buyerId}/{@code sellerId} — the actual
 * recipient is resolved through {@link UserClient}. If user-service is
 * unreachable the {@code FallbackUserClient} returns {@code null} and the
 * listener logs a warning and skips the email.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final EmailService emailService;
    private final UserClient userClient;

    private static final String GROUP_ID = "notification-group";

    // ------------------------------------------------------------ created

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = GROUP_ID)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: orderId={}, sellerId={}", event.orderId(), event.sellerId());
        UserDto seller = userClient.getUserById(event.sellerId());
        String recipient = resolveRecipient(seller, event.sellerId());
        if (seller == null) {
            log.warn("Skipping NEW_ORDER_SELLER email — user-service unreachable for sellerId={}", event.sellerId());
            return;
        }
        Map<String, Object> model = new HashMap<>();
        model.put("orderId", event.orderId());
        model.put("amount", event.amount());
        emailService.send(NEW_ORDER_SELLER, recipient, model);
    }

    // -------------------------------------------------------------- paid

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.ORDER_PAID, groupId = GROUP_ID)
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("Received OrderPaidEvent: orderId={}, buyerId={}", event.orderId(), event.buyerId());
        UserDto buyer = userClient.getUserById(event.buyerId());
        String recipient = resolveRecipient(buyer, event.buyerId());
        if (buyer == null) {
            log.warn("Skipping ORDER_PAID email — user-service unreachable for buyerId={}", event.buyerId());
            return;
        }
        Map<String, Object> model = Map.of("orderId", event.orderId());
        emailService.send(ORDER_PAID, recipient, model);
    }

    // ---------------------------------------------------------- cancelled

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED, groupId = GROUP_ID)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received OrderCancelledEvent: orderId={}, buyerId={}, sellerId={}",
                event.orderId(), event.buyerId(), event.sellerId());
        UserDto buyer = userClient.getUserById(event.buyerId());
        if (buyer == null) {
            log.warn("Skipping ORDER_CANCELLED buyer email — user-service unreachable for buyerId={}",
                    event.buyerId());
        } else {
            Map<String, Object> model = Map.of(
                    "orderId", event.orderId(),
                    "reason", event.reason() == null ? "не указана" : event.reason()
            );
            emailService.send(ORDER_CANCELLED, buyer.email(), model);
        }
    }

    // ------------------------------------------------------------- failed

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.ORDER_FAILED, groupId = GROUP_ID)
    public void onOrderFailed(OrderFailedEvent event) {
        // Order-failed is a saga-internal compensation trigger; we simply log
        // it. The matching compensation in product-service handles the state
        // transition. No customer-facing email in MVP.
        log.info("Received OrderFailedEvent: orderId={}, buyerId={}, reason={}",
                event.orderId(), event.buyerId(), event.reason());
    }

    // --------------------------------------------------------------- DLT

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED + KafkaTopics.DLT_SUFFIX, groupId = "notification-dlt")
    public void onOrderCreatedDlt(OrderCreatedEvent event) {
        log.error("DLT for ORDER_CREATED: orderId={}, sellerId={} — manual inspection required",
                event.orderId(), event.sellerId());
    }

    @KafkaListener(topics = KafkaTopics.ORDER_PAID + KafkaTopics.DLT_SUFFIX, groupId = "notification-dlt")
    public void onOrderPaidDlt(OrderPaidEvent event) {
        log.error("DLT for ORDER_PAID: orderId={}, buyerId={} — manual inspection required",
                event.orderId(), event.buyerId());
    }

    @KafkaListener(topics = KafkaTopics.ORDER_CANCELLED + KafkaTopics.DLT_SUFFIX, groupId = "notification-dlt")
    public void onOrderCancelledDlt(OrderCancelledEvent event) {
        log.error("DLT for ORDER_CANCELLED: orderId={}, buyerId={} — manual inspection required",
                event.orderId(), event.buyerId());
    }

    @KafkaListener(topics = KafkaTopics.ORDER_FAILED + KafkaTopics.DLT_SUFFIX, groupId = "notification-dlt")
    public void onOrderFailedDlt(OrderFailedEvent event) {
        log.error("DLT for ORDER_FAILED: orderId={}, buyerId={} — manual inspection required",
                event.orderId(), event.buyerId());
    }

    // ----------------------------------------------------------- helpers

    /**
     * Returns the user's email, or {@code "<id>@unknown"} when the lookup
     * failed (defensive — callers should already have null-checked before
     * calling this).
     */
    private static String resolveRecipient(UserDto user, Long id) {
        return user != null && user.email() != null ? user.email() : id + UNKNOWN_EMAIL_SUFFIX;
    }
}
