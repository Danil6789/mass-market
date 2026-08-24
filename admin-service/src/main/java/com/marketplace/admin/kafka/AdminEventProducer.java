package com.marketplace.admin.kafka;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.UserBlockedEvent;
import com.marketplace.common.event.UserUnblockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes admin-initiated user lifecycle events. user-service consumes
 * these and deactivates / reactivates the account in its own DB.
 *
 * <p>The user id is used as the partition key so events for the same user
 * land on the same partition (preserves ordering of block vs unblock).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<?> publishUserBlocked(UserBlockedEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.USER_BLOCKED, String.valueOf(event.userId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish UserBlockedEvent: userId={}", event.userId(), ex);
                    } else {
                        log.info("Published UserBlockedEvent: userId={}, reason={}, partition={}, offset={}",
                                event.userId(),
                                event.reason(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public CompletableFuture<?> publishUserUnblocked(UserUnblockedEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.USER_UNBLOCKED, String.valueOf(event.userId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish UserUnblockedEvent: userId={}", event.userId(), ex);
                    } else {
                        log.info("Published UserUnblockedEvent: userId={}, partition={}, offset={}",
                                event.userId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
