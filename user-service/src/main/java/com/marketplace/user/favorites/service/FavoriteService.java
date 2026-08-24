package com.marketplace.user.favorites.service;

import com.marketplace.user.entity.Favorite;
import com.marketplace.user.exception.UserNotFoundException;
import com.marketplace.user.favorites.dto.FavoriteResponse;
import com.marketplace.user.favorites.exception.FavoriteNotFoundException;
import com.marketplace.user.mapper.FavoriteMapper;
import com.marketplace.user.repository.FavoriteRepository;
import com.marketplace.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.marketplace.user.constant.ExceptionMessages.FAVORITE_ALREADY_EXISTS;
import static com.marketplace.user.constant.ExceptionMessages.FAVORITE_NOT_FOUND;
import static com.marketplace.user.constant.ExceptionMessages.USER_NOT_FOUND;

/**
 * Manages the favourites list for the current user. Product references are
 * stored as {@link Long productId} only — there is no FK to product-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final FavoriteMapper favoriteMapper;

    @Transactional(readOnly = true)
    public List<FavoriteResponse> list(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(USER_NOT_FOUND);
        }
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(favoriteMapper::toResponse)
                .toList();
    }

    @Transactional
    public FavoriteResponse add(Long userId, Long productId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(USER_NOT_FOUND);
        }
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            // Idempotent: return the existing entry instead of failing.
            Favorite existing = favoriteRepository.findByUserIdAndProductId(userId, productId).orElseThrow();
            return favoriteMapper.toResponse(existing);
        }
        Favorite favorite = Favorite.builder()
                .userId(userId)
                .productId(productId)
                .build();
        try {
            Favorite saved = favoriteRepository.saveAndFlush(favorite);
            log.info("Favorite added: userId={}, productId={}", userId, productId);
            return favoriteMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert — treat as success (idempotent).
            Favorite existing = favoriteRepository.findByUserIdAndProductId(userId, productId)
                    .orElseThrow(() -> new FavoriteNotFoundException(FAVORITE_ALREADY_EXISTS));
            return favoriteMapper.toResponse(existing);
        }
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        if (!favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new FavoriteNotFoundException(FAVORITE_NOT_FOUND);
        }
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("Favorite removed: userId={}, productId={}", userId, productId);
    }
}