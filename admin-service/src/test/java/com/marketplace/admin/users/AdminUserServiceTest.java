package com.marketplace.admin.users;

import com.marketplace.admin.audit.service.AuditService;
import com.marketplace.admin.client.UserClient;
import com.marketplace.admin.client.UserDto;
import com.marketplace.admin.constant.AdminActions;
import com.marketplace.admin.constant.TargetType;
import com.marketplace.admin.entity.AuditLog;
import com.marketplace.admin.kafka.AdminEventProducer;
import com.marketplace.admin.users.exception.UserNotFoundException;
import com.marketplace.admin.users.service.AdminUserService;
import com.marketplace.common.event.UserBlockedEvent;
import com.marketplace.common.event.UserUnblockedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminUserService}. The Feign client and the Kafka
 * producer are mocked; {@link AuditService} is also mocked so the test
 * verifies only the orchestration logic.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserClient userClient;
    @Mock private AdminEventProducer adminEventProducer;
    @Mock private AuditService auditService;

    @InjectMocks private AdminUserService adminUserService;

    @Test
    void blockUser_publishesEventAndWritesAudit() {
        when(userClient.getUserById(42L)).thenReturn(new UserDto(42L, "alice@example.com", false));

        adminUserService.blockUser(42L, "spam", 1L);

        ArgumentCaptor<UserBlockedEvent> eventCaptor = ArgumentCaptor.forClass(UserBlockedEvent.class);
        verify(adminEventProducer).publishUserBlocked(eventCaptor.capture());
        UserBlockedEvent captured = eventCaptor.getValue();
        assertThat(captured.userId()).isEqualTo(42L);
        assertThat(captured.blockedBy()).isEqualTo(1L);
        assertThat(captured.reason()).isEqualTo("spam");
        assertThat(captured.timestamp()).isNotNull();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).record(auditCaptor.capture());
        AuditLog saved = auditCaptor.getValue();
        assertThat(saved.getAdminId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo(AdminActions.BLOCK_USER);
        assertThat(saved.getTargetType()).isEqualTo(TargetType.USER);
        assertThat(saved.getTargetId()).isEqualTo(42L);
        assertThat(saved.getDetails()).isEqualTo("reason=spam");
    }

    @Test
    void blockUser_throwsWhenUserMissing() {
        when(userClient.getUserById(7L)).thenReturn(null);

        assertThatThrownBy(() -> adminUserService.blockUser(7L, "bad", 1L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void unblockUser_publishesEventAndWritesAudit() {
        when(userClient.getUserById(99L)).thenReturn(new UserDto(99L, "bob@example.com", true));

        adminUserService.unblockUser(99L, 1L);

        ArgumentCaptor<UserUnblockedEvent> eventCaptor = ArgumentCaptor.forClass(UserUnblockedEvent.class);
        verify(adminEventProducer).publishUserUnblocked(eventCaptor.capture());
        UserUnblockedEvent captured = eventCaptor.getValue();
        assertThat(captured.userId()).isEqualTo(99L);
        assertThat(captured.unblockedBy()).isEqualTo(1L);
        assertThat(captured.timestamp()).isNotNull();

        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditService).record(auditCaptor.capture());
        AuditLog saved = auditCaptor.getValue();
        assertThat(saved.getAdminId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo(AdminActions.UNBLOCK_USER);
        assertThat(saved.getTargetType()).isEqualTo(TargetType.USER);
        assertThat(saved.getTargetId()).isEqualTo(99L);
    }
}
