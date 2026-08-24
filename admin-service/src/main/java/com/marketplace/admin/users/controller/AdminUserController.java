package com.marketplace.admin.users.controller;

import com.marketplace.admin.security.AuthenticatedUser;
import com.marketplace.admin.users.api.AdminUserApi;
import com.marketplace.admin.users.dto.BlockUserRequest;
import com.marketplace.admin.users.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for admin user moderation. Delegates to
 * {@link AdminUserService} and resolves the calling admin from the security
 * context. Returns 202 Accepted because the actual deactivation happens
 * asynchronously via Kafka.
 */
@RestController
@RequiredArgsConstructor
public class AdminUserController implements AdminUserApi {

    private final AdminUserService adminUserService;

    @Override
    public ResponseEntity<Void> blockUser(Object principal, Long id, BlockUserRequest request) {
        AuthenticatedUser admin = resolveAdmin(principal);
        adminUserService.blockUser(id, request.getReason(), admin.id());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Override
    public ResponseEntity<Void> unblockUser(Object principal, Long id) {
        AuthenticatedUser admin = resolveAdmin(principal);
        adminUserService.unblockUser(id, admin.id());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    private static AuthenticatedUser resolveAdmin(Object principal) {
        if (!(principal instanceof AuthenticatedUser au) || !au.isAdmin()) {
            throw new AccessDeniedException("Требуется роль ADMIN");
        }
        return au;
    }
}
