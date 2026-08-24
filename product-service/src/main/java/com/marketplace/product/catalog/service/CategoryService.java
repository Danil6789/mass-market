package com.marketplace.product.catalog.service;

import com.marketplace.product.catalog.dto.CategoryResponse;
import com.marketplace.product.catalog.dto.CreateCategoryRequest;
import com.marketplace.product.catalog.exception.CategoryAlreadyExistsException;
import com.marketplace.product.catalog.exception.CategoryNotFoundException;
import com.marketplace.product.entity.Category;
import com.marketplace.product.mapper.CategoryMapper;
import com.marketplace.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.marketplace.product.constant.ExceptionMessages.CATEGORY_ALREADY_EXISTS;
import static com.marketplace.product.constant.ExceptionMessages.CATEGORY_NOT_FOUND;

/**
 * Catalog / category operations. Flat list for MVP; tree assembly can be
 * layered on later via a single SELECT + in-memory grouping.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (request.getParentId() != null
                && !categoryRepository.existsById(request.getParentId())) {
            throw new CategoryNotFoundException(CATEGORY_NOT_FOUND);
        }

        Category category = categoryMapper.toEntity(request);
        try {
            Category saved = categoryRepository.saveAndFlush(category);
            log.info("Category created: id={}, name={}", saved.getId(), saved.getName());
            return categoryMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // Race-condition fallback: same (parent_id, name) inserted concurrently.
            log.warn("Duplicate category detected at insert time: {}", request.getName());
            throw new CategoryAlreadyExistsException(CATEGORY_ALREADY_EXISTS);
        }
    }
}