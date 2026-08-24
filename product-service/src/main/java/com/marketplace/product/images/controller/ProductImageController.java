package com.marketplace.product.images.controller;

import com.marketplace.product.constant.ExceptionMessages;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductImage;
import com.marketplace.product.exception.ForbiddenException;
import com.marketplace.product.images.api.ProductImageApi;
import com.marketplace.product.images.dto.ImageUploadResponse;
import com.marketplace.product.images.service.ImageStorageService;
import com.marketplace.product.mapper.ProductImageMapper;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.marketplace.product.constant.ExceptionMessages.ACCESS_DENIED;
import static com.marketplace.product.constant.ExceptionMessages.NOT_OWNER;
import static com.marketplace.product.constant.ExceptionMessages.PRODUCT_NOT_FOUND;

/**
 * REST controller for image endpoints. Delegates to
 * {@link ImageStorageService} and enforces ownership / admin rules on
 * uploads.
 */
@RestController
@RequiredArgsConstructor
public class ProductImageController implements ProductImageApi {

    private final ImageStorageService imageStorageService;
    private final ProductRepository productRepository;
    private final ProductImageMapper productImageMapper;

    @Override
    public ResponseEntity<ImageUploadResponse> upload(Object principal, Long productId, MultipartFile file) {
        AuthenticatedUser user = resolveAuthenticated(principal);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new com.marketplace.product.products.exception.ProductNotFoundException(PRODUCT_NOT_FOUND));
        if (!user.isAdmin() && !product.getSellerId().equals(user.id())) {
            throw new ForbiddenException(NOT_OWNER);
        }

        ProductImage saved = imageStorageService.upload(productId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(productImageMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<List<ImageUploadResponse>> list(Long productId) {
        List<ProductImage> images = imageStorageService.list(productId);
        List<ImageUploadResponse> response = images.stream()
                .map(productImageMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private static AuthenticatedUser resolveAuthenticated(Object principal) {
        if (!(principal instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        return au;
    }
}