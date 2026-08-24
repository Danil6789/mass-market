package com.marketplace.user.mapper;

import com.marketplace.user.entity.Favorite;
import com.marketplace.user.favorites.dto.FavoriteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for {@link Favorite} → {@link FavoriteResponse}.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface FavoriteMapper {

    FavoriteResponse toResponse(Favorite favorite);
}