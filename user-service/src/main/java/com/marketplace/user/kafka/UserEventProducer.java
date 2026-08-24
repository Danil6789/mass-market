package com.marketplace.user.kafka;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes user-related events to Kafka.
 *
 * <p>Uses the topic name from {@link KafkaTopics} and the user id as the
 * partition key so all events for the same user land on the same partition
 * (preserves per-user ordering).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<?> publishUserRegistered(UserRegisteredEvent event) {
        return kafkaTemplate
                .send(KafkaTopics.USER_REGISTERED, String.valueOf(event.userId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish UserRegisteredEvent: userId={}", event.userId(), ex);
                    } else {
                        log.debug("Published UserRegisteredEvent: userId={}, partition={}, offset={}",
                                event.userId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}