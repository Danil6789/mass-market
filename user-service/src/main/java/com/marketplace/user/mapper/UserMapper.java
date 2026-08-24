package com.marketplace.user.mapper;

import com.marketplace.user.auth.dto.AuthResponse;
import com.marketplace.user.auth.dto.RegisterRequest;
import com.marketplace.user.entity.User;
import com.marketplace.user.profile.dto.UserProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for User entity → DTOs.
 *
 * <p>{@code componentModel = SPRING} so the bean is auto-registered.
 * {@code unmappedTargetPolicy = IGNORE} keeps the build green if MapStruct
 * can't fill every target field — we only care about explicit mappings.</p>
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    /**
     * Convert a registration payload into a {@link User} entity. The caller
     * is responsible for encoding the password and assigning the role
     * (default USER) before persisting.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", source = "password")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "blocked", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest request);

    UserProfileResponse toResponse(User user);

    /**
     * Build an {@link AuthResponse} by combining the user profile with the
     * freshly issued JWT pair. Implemented as a default method because
     * MapStruct does not generate parameterised factory methods automatically.
     */
    default AuthResponse toAuthResponse(User user,
                                        String accessToken,
                                        String refreshToken,
                                        long expiresIn) {
        return new AuthResponse(accessToken, refreshToken, expiresIn, "Bearer",
                toResponse(user));
    }
}