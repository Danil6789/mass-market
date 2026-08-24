package com.marketplace.notification.kafka;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.ProductDeletedEvent;
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

import java.util.Map;

import static com.marketplace.notification.constant.EventTypes.PRODUCT_DELETED;

/**
 * Listens to product events. Only {@code product.deleted} triggers an email —
 * it informs the seller that one of their listings was removed (typically by
 * an admin).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final EmailService emailService;
    private final UserClient userClient;

    private static final String GROUP_ID = "notification-group";

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.PRODUCT_DELETED, groupId = GROUP_ID)
    public void onProductDeleted(ProductDeletedEvent event) {
        log.info("Received ProductDeletedEvent: productId={}, sellerId={}",
                event.productId(), event.sellerId());
        UserDto seller = userClient.getUserById(event.sellerId());
        if (seller == null) {
            log.warn("Skipping PRODUCT_DELETED email — user-service unreachable for sellerId={}",
                    event.sellerId());
            return;
        }
        Map<String, Object> model = Map.of("productId", event.productId());
        emailService.send(PRODUCT_DELETED, seller.email(), model);
    }

    @KafkaListener(topics = KafkaTopics.PRODUCT_DELETED + KafkaTopics.DLT_SUFFIX, groupId = "notification-dlt")
    public void onProductDeletedDlt(ProductDeletedEvent event) {
        log.error("DLT for PRODUCT_DELETED: productId={}, sellerId={} — manual inspection required",
                event.productId(), event.sellerId());
    }
}
