package com.marketplace.product.mapper;

import com.marketplace.product.entity.Product;
import com.marketplace.product.products.dto.ProductCreateRequest;
import com.marketplace.product.products.dto.ProductResponse;
import com.marketplace.product.products.dto.ProductSummary;
import com.marketplace.product.products.dto.ProductUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for {@link Product} ↔ DTOs.
 *
 * <p>{@code NullValuePropertyMappingStrategy.IGNORE} on the partial-update
 * mapping allows {@code null} fields in the request to be skipped.</p>
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellerId", ignore = true)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ProductUpdateRequest request, @MappingTarget Product product);

    ProductResponse toResponse(Product product);

    ProductSummary toSummary(Product product);
}