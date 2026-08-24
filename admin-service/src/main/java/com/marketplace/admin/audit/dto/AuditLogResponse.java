package com.marketplace.admin.audit.dto;

import com.marketplace.admin.constant.AdminActions;
import com.marketplace.admin.constant.TargetType;

import java.time.Instant;

/**
 * Response payload for the audit-log endpoint. Java record (immutable)
 * per marketplace convention for response DTOs.
 */
public record AuditLogResponse(
        Long id,
        Long adminId,
        AdminActions action,
        TargetType targetType,
        Long targetId,
        String details,
        Instant occurredAt
) {
}
