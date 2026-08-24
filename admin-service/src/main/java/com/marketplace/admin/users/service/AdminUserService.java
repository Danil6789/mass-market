package com.marketplace.admin.users.service;

import com.marketplace.admin.audit.service.AuditService;
import com.marketplace.admin.client.UserClient;
import com.marketplace.admin.client.UserDto;
import com.marketplace.admin.constant.AdminActions;
import com.marketplace.admin.constant.TargetType;
import com.marketplace.admin.entity.AuditLog;
import com.marketplace.admin.kafka.AdminEventProducer;
import com.marketplace.admin.users.exception.UserNotFoundException;
import com.marketplace.common.event.UserBlockedEvent;
import com.marketplace.common.event.UserUnblockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Coordinates admin moderation actions against users: verifies the user
 * exists via the user-service Feign client, publishes a Kafka event so
 * user-service deactivates / reactivates the account asynchronously, and
 * writes an audit row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserClient userClient;
    private final AdminEventProducer adminEventProducer;
    private final AuditService auditService;

    /**
     * Publish {@code user.blocked} and record the moderation action.
     * Throws {@link UserNotFoundException} when user-service responds with
     * 404 (Feign fallback returns {@code null}).
     */
    @Transactional
    public void blockUser(Long userId, String reason, Long adminId) {
        UserDto user = userClient.getUserById(userId);
        if (user == null) {
            throw UserNotFoundException.forId(userId);
        }

        UserBlockedEvent event = new UserBlockedEvent(userId, adminId, reason, Instant.now());
        adminEventProducer.publishUserBlocked(event);

        AuditLog row = AuditLog.builder()
                .adminId(adminId)
                .action(AdminActions.BLOCK_USER)
                .targetType(TargetType.USER)
                .targetId(userId)
                .details("reason=" + reason)
                .build();
        auditService.record(row);
        log.info("Admin {} blocked user {}", adminId, userId);
    }

    @Transactional
    public void unblockUser(Long userId, Long adminId) {
        UserDto user = userClient.getUserById(userId);
        if (user == null) {
            throw UserNotFoundException.forId(userId);
        }

        UserUnblockedEvent event = new UserUnblockedEvent(userId, adminId, Instant.now());
        adminEventProducer.publishUserUnblocked(event);

        AuditLog row = AuditLog.builder()
                .adminId(adminId)
                .action(AdminActions.UNBLOCK_USER)
                .targetType(TargetType.USER)
                .targetId(userId)
                .build();
        auditService.record(row);
        log.info("Admin {} unblocked user {}", adminId, userId);
    }
}
