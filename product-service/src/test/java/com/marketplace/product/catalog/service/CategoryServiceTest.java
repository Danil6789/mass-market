package com.marketplace.product.catalog.service;

import com.marketplace.product.catalog.dto.CategoryResponse;
import com.marketplace.product.catalog.dto.CreateCategoryRequest;
import com.marketplace.product.catalog.exception.CategoryAlreadyExistsException;
import com.marketplace.product.catalog.exception.CategoryNotFoundException;
import com.marketplace.product.entity.Category;
import com.marketplace.product.mapper.CategoryMapper;
import com.marketplace.product.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CategoryService}. All collaborators are mocked.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks private CategoryService categoryService;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .parentId(null)
                .createdAt(Instant.now())
                .build();
    }

    // ------------------------------------------------------------------ list

    @Test
    void list_returnsMappedCategories() {
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(sampleCategory));
        when(categoryMapper.toResponse(sampleCategory)).thenReturn(
                new CategoryResponse(1L, "Electronics", null, sampleCategory.getCreatedAt()));

        List<CategoryResponse> result = categoryService.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Electronics");
    }

    // ------------------------------------------------------------------ create

    @Test
    void create_persistsCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setParentId(null);

        when(categoryMapper.toEntity(request)).thenReturn(Category.builder().name("Electronics").build());
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            c.setCreatedAt(Instant.now());
            return c;
        });
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(
                new CategoryResponse(1L, "Electronics", null, Instant.now()));

        CategoryResponse response = categoryService.create(request);

        assertThat(response.name()).isEqualTo("Electronics");
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void create_throwsCategoryNotFound_whenParentMissing() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Phones");
        request.setParentId(99L);

        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void create_throwsCategoryAlreadyExists_onRaceCondition() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setParentId(null);

        when(categoryMapper.toEntity(request)).thenReturn(Category.builder().name("Electronics").build());
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(CategoryAlreadyExistsException.class);
    }
}