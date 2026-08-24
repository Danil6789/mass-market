package com.marketplace.admin.audit.mapper;

import com.marketplace.admin.audit.dto.AuditLogResponse;
import com.marketplace.admin.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper between {@link AuditLog} (entity) and {@link AuditLogResponse}
 * (DTO). Spring-component model; unmapped targets are ignored to avoid noise
 * when the entity is extended later.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog entity);
}
