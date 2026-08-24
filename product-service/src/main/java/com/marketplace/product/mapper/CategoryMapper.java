package com.marketplace.product.mapper;

import com.marketplace.product.catalog.dto.CategoryResponse;
import com.marketplace.product.catalog.dto.CreateCategoryRequest;
import com.marketplace.product.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for {@link Category} ↔ DTOs.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    CategoryResponse toResponse(Category category);
}