package com.marketplace.notification.logs.mapper;

import com.marketplace.notification.email.EmailLog;
import com.marketplace.notification.logs.dto.EmailLogResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper between {@link EmailLog} (entity) and {@link EmailLogResponse}
 * (DTO). Spring-component model; unmapped targets are ignored to avoid noise
 * when the entity is extended later.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EmailLogMapper {

    EmailLogResponse toResponse(EmailLog entity);
}
