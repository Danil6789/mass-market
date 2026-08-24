package com.marketplace.admin.audit;

import com.marketplace.admin.audit.dto.AuditLogResponse;
import com.marketplace.admin.audit.mapper.AuditLogMapper;
import com.marketplace.admin.audit.service.AuditService;
import com.marketplace.admin.constant.AdminActions;
import com.marketplace.admin.constant.TargetType;
import com.marketplace.admin.entity.AuditLog;
import com.marketplace.admin.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditService}. The {@link AuditLogMapper} interface
 * is mocked because MapStruct's generated implementation is not on the
 * test classpath (same pattern used by notification-service).
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditLogMapper auditLogMapper;

    @InjectMocks private AuditService auditService;

    @Test
    void record_persistsAuditRow() {
        AuditLog row = AuditLog.builder()
                .adminId(1L)
                .action(AdminActions.BLOCK_USER)
                .targetType(TargetType.USER)
                .targetId(5L)
                .details("reason=spam")
                .build();
        when(auditLogRepository.save(row)).thenReturn(row);

        AuditLog saved = auditService.record(row);

        assertThat(saved).isSameAs(row);
        verify(auditLogRepository).save(row);
    }

    @Test
    void list_returnsMappedPage() {
        AuditLog row = AuditLog.builder()
                .id(11L)
                .adminId(1L)
                .action(AdminActions.DELETE_PRODUCT)
                .targetType(TargetType.PRODUCT)
                .targetId(7L)
                .details("admin removed")
                .occurredAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        AuditLogResponse mapped = new AuditLogResponse(
                11L, 1L, AdminActions.DELETE_PRODUCT, TargetType.PRODUCT, 7L,
                "admin removed", Instant.parse("2024-01-01T00:00:00Z"));
        when(auditLogMapper.toResponse(row)).thenReturn(mapped);

        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.findAllByOrderByOccurredAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(row), pageable, 1));

        Page<AuditLogResponse> result = auditService.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isSameAs(mapped);
        verify(auditLogMapper).toResponse(row);
    }
}
