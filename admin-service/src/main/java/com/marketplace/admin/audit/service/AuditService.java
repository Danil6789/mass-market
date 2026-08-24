package com.marketplace.admin.audit.service;

import com.marketplace.admin.audit.dto.AuditLogResponse;
import com.marketplace.admin.audit.mapper.AuditLogMapper;
import com.marketplace.admin.entity.AuditLog;
import com.marketplace.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only audit log service. Used both by the admin moderation endpoints
 * (to record BLOCK_USER / UNBLOCK_USER / DELETE_PRODUCT) and by the
 * {@code product.created} Kafka consumer (to auto-audit product creation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    /**
     * Persist a new audit row. Returns the saved entity so callers can pass
     * the generated id to downstream concerns (e.g. logging).
     */
    @Transactional
    public AuditLog record(AuditLog auditLog) {
        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("Recorded audit: id={}, action={}, targetType={}, targetId={}, adminId={}",
                saved.getId(), saved.getAction(), saved.getTargetType(), saved.getTargetId(), saved.getAdminId());
        return saved;
    }

    /**
     * Paginated list of audit rows, newest first.
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(Pageable pageable) {
        return auditLogRepository.findAllByOrderByOccurredAtDesc(pageable)
                .map(auditLogMapper::toResponse);
    }
}
