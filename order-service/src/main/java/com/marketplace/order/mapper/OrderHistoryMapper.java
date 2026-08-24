package com.marketplace.order.mapper;

import com.marketplace.order.entity.OrderHistory;
import com.marketplace.order.orders.dto.OrderHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for {@link OrderHistory} → {@link OrderHistoryResponse}.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrderHistoryMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "changedBy", source = "changedBy")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "changedAt", source = "changedAt")
    OrderHistoryResponse toResponse(OrderHistory history);
}
