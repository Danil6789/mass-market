package com.marketplace.order.mapper;

import com.marketplace.order.entity.Order;
import com.marketplace.order.orders.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for {@link Order} ↔ DTOs.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "buyerId", ignore = true)
    @Mapping(target = "sellerId", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(com.marketplace.order.orders.dto.CreateOrderRequest request);

    OrderResponse toResponse(Order order);
}
