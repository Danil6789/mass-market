package com.marketplace.admin.products.controller;

import com.marketplace.admin.products.api.AdminProductApi;
import com.marketplace.admin.products.service.AdminProductService;
import com.marketplace.admin.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for admin product moderation.
 */
@RestController
@RequiredArgsConstructor
public class AdminProductController implements AdminProductApi {

    private final AdminProductService adminProductService;

    @Override
    public ResponseEntity<Void> delete(Object principal, Long id) {
        AuthenticatedUser admin = resolveAdmin(principal);
        adminProductService.deleteProduct(id, admin.id());
        return ResponseEntity.noContent().build();
    }

    private static AuthenticatedUser resolveAdmin(Object principal) {
        if (!(principal instanceof AuthenticatedUser au) || !au.isAdmin()) {
            throw new AccessDeniedException("Требуется роль ADMIN");
        }
        return au;
    }
}
