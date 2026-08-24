package com.marketplace.user.kafka;

import com.marketplace.common.constant.KafkaTopics;
import com.marketplace.common.event.UserBlockedEvent;
import com.marketplace.common.event.UserUnblockedEvent;
import com.marketplace.user.entity.User;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Listens to administrative user events and applies them locally:
 * {@code user.blocked} → set {@code active=false}; {@code user.unblocked} →
 * set {@code active=true} and clear {@code blocked}.
 *
 * <p>Wrapped with {@link RetryableTopic} so transient failures are retried
 * automatically before the message is sent to the DLT.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserRepository userRepository;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.USER_BLOCKED, groupId = "user-blocked-group")
    public void onUserBlocked(UserBlockedEvent event) {
        log.info("Received UserBlockedEvent: userId={}, blockedBy={}",
                event.userId(), event.blockedBy());

        Optional<User> maybe = userRepository.findById(event.userId());
        if (maybe.isEmpty()) {
            log.warn("User not found for UserBlockedEvent: userId={} — ignoring", event.userId());
            return;
        }
        User user = maybe.get();
        user.setBlocked(true);
        user.setActive(false);
        userRepository.save(user);
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(topics = KafkaTopics.USER_UNBLOCKED, groupId = "user-unblocked-group")
    public void onUserUnblocked(UserUnblockedEvent event) {
        log.info("Received UserUnblockedEvent: userId={}", event.userId());

        Optional<User> maybe = userRepository.findById(event.userId());
        if (maybe.isEmpty()) {
            log.warn("User not found for UserUnblockedEvent: userId={} — ignoring", event.userId());
            return;
        }
        User user = maybe.get();
        user.setBlocked(false);
        user.setActive(true);
        userRepository.save(user);
    }

    @KafkaListener(topics = KafkaTopics.USER_BLOCKED + KafkaTopics.DLT_SUFFIX,
            groupId = "user-blocked-dlt")
    public void onUserBlockedDlt(UserBlockedEvent event) {
        log.error("DLT for USER_BLOCKED: userId={} — manual inspection required", event.userId());
    }

    @KafkaListener(topics = KafkaTopics.USER_UNBLOCKED + KafkaTopics.DLT_SUFFIX,
            groupId = "user-unblocked-dlt")
    public void onUserUnblockedDlt(UserUnblockedEvent event) {
        log.error("DLT for USER_UNBLOCKED: userId={} — manual inspection required", event.userId());
    }
}