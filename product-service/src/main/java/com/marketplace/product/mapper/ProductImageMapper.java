package com.marketplace.product.mapper;

import com.marketplace.product.entity.ProductImage;
import com.marketplace.product.images.dto.ImageUploadResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for {@link ProductImage} → {@link ImageUploadResponse}.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductImageMapper {

    @Mapping(target = "imageId", source = "id")
    ImageUploadResponse toResponse(ProductImage image);
}