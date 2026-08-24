package com.marketplace.user.profile.controller;

import com.marketplace.user.profile.api.UserApi;
import com.marketplace.user.profile.dto.UpdateProfileRequest;
import com.marketplace.user.profile.dto.UserProfileResponse;
import com.marketplace.user.profile.service.UserService;
import com.marketplace.user.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import static com.marketplace.user.constant.ExceptionMessages.ACCESS_DENIED;

/**
 * REST controller for the profile endpoints. Delegates to {@link UserService}
 * and enforces ownership/admin rules on the public {@code /api/users/{id}}
 * endpoint.
 */
@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<UserProfileResponse> getCurrent(Object principal) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(userService.getCurrent(userId));
    }

    @Override
    public ResponseEntity<UserProfileResponse> updateCurrent(Object principal, UpdateProfileRequest request) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(userService.update(userId, request));
    }

    @Override
    public ResponseEntity<UserProfileResponse> getById(Long id, Object principal) {
        AuthenticatedUser caller = resolveAuthenticated(principal);
        if (!caller.isAdmin() && !caller.id().equals(id)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        return ResponseEntity.ok(userService.getById(id));
    }

    private static Long resolveUserId(Object principal) {
        return resolveAuthenticated(principal).id();
    }

    private static AuthenticatedUser resolveAuthenticated(Object principal) {
        if (!(principal instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        return au;
    }
}