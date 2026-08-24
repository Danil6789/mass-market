package com.marketplace.user.favorites.controller;

import com.marketplace.user.favorites.api.FavoriteApi;
import com.marketplace.user.favorites.dto.AddFavoriteRequest;
import com.marketplace.user.favorites.dto.FavoriteResponse;
import com.marketplace.user.favorites.service.FavoriteService;
import com.marketplace.user.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.marketplace.user.constant.ExceptionMessages.ACCESS_DENIED;

/**
 * REST controller for the favourites endpoints. Delegates to
 * {@link FavoriteService} and resolves the current user's id from the
 * security context.
 */
@RestController
@RequiredArgsConstructor
public class FavoriteController implements FavoriteApi {

    private final FavoriteService favoriteService;

    @Override
    public ResponseEntity<FavoriteResponse> add(Object principal, AddFavoriteRequest body) {
        Long userId = resolveUserId(principal);
        FavoriteResponse created = favoriteService.add(userId, body.getProductId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    public ResponseEntity<List<FavoriteResponse>> list(Object principal) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.ok(favoriteService.list(userId));
    }

    @Override
    public ResponseEntity<Void> remove(Object principal, Long productId) {
        Long userId = resolveUserId(principal);
        favoriteService.remove(userId, productId);
        return ResponseEntity.noContent().build();
    }

    private static Long resolveUserId(Object principal) {
        if (!(principal instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        return au.id();
    }
}