package com.marketplace.product.catalog.controller;

import com.marketplace.product.catalog.api.CategoryApi;
import com.marketplace.product.catalog.dto.CategoryResponse;
import com.marketplace.product.catalog.dto.CreateCategoryRequest;
import com.marketplace.product.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for category endpoints. Delegates to {@link CategoryService}.
 */
@RestController
@RequiredArgsConstructor
public class CategoryController implements CategoryApi {

    private final CategoryService categoryService;

    @Override
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(categoryService.list());
    }

    @Override
    public ResponseEntity<CategoryResponse> create(CreateCategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}