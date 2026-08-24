package com.marketplace.product.products.controller;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.product.products.api.ProductApi;
import com.marketplace.product.products.dto.ProductCreateRequest;
import com.marketplace.product.products.dto.ProductFilter;
import com.marketplace.product.products.dto.ProductResponse;
import com.marketplace.product.products.dto.ProductSummary;
import com.marketplace.product.products.dto.ProductUpdateRequest;
import com.marketplace.product.products.service.ProductService;
import com.marketplace.product.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

import static com.marketplace.product.constant.ExceptionMessages.ACCESS_DENIED;

/**
 * REST controller for product endpoints. Delegates to {@link ProductService}
 * and resolves the current user from the security context.
 */
@RestController
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductService productService;

    @Override
    public ResponseEntity<PageResponse<ProductSummary>> list(Long categoryId,
                                                             BigDecimal minPrice,
                                                             BigDecimal maxPrice,
                                                             ProductStatus status,
                                                             String search,
                                                             Long sellerId,
                                                             Pageable pageable) {
        ProductFilter filter = new ProductFilter(categoryId, minPrice, maxPrice, status, search, sellerId);
        return ResponseEntity.ok(productService.search(filter, pageable));
    }

    @Override
    public ResponseEntity<List<ProductSummary>> listBySeller(Long userId) {
        return ResponseEntity.ok(productService.listBySeller(userId));
    }

    @Override
    public ResponseEntity<ProductResponse> getById(Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @Override
    public ResponseEntity<ProductResponse> create(Object principal, ProductCreateRequest request) {
        AuthenticatedUser user = resolveAuthenticated(principal);
        ProductResponse response = productService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ProductResponse> update(Object principal, Long id, ProductUpdateRequest request) {
        AuthenticatedUser user = resolveAuthenticated(principal);
        return ResponseEntity.ok(productService.update(id, request, user));
    }

    @Override
    public ResponseEntity<Void> delete(Object principal, Long id) {
        AuthenticatedUser user = resolveAuthenticated(principal);
        productService.softDelete(id, user);
        return ResponseEntity.noContent().build();
    }

    private static AuthenticatedUser resolveAuthenticated(Object principal) {
        if (!(principal instanceof AuthenticatedUser au)) {
            throw new AccessDeniedException(ACCESS_DENIED);
        }
        return au;
    }
}