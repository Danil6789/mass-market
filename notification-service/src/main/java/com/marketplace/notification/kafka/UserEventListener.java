package com.marketplace.notification.kafka;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.UserRegisteredEvent;
import com.marketplace.notification.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Listens to user events and sends the matching transactional emails.
 *
 * <p>{@code UserRegisteredEvent} already carries the recipient's email so no
 * Feign lookup is necessary.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;

    private static final String GROUP_ID = "notification-group";

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = GROUP_ID)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: userId={}, email={}", event.userId(), event.email());
        Map<String, Object> model = Map.of(
                "name", event.name() == null ? "" : event.name(),
                "email", event.email() == null ? "" : event.email()
        );
        emailService.send("WELCOME", event.email(), model);
    }

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED + KafkaTopics.DLT_SUFFIX, groupId = "notification-dlt")
    public void onUserRegisteredDlt(UserRegisteredEvent event) {
        log.error("DLT for USER_REGISTERED: userId={}, email={} — manual inspection required",
                event.userId(), event.email());
    }
}
