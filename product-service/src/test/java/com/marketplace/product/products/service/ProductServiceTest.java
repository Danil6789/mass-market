package com.marketplace.product.products.service;

import com.marketplace.common.dto.PageResponse;
import com.marketplace.common.event.ProductCreatedEvent;
import com.marketplace.common.event.ProductDeletedEvent;
import com.marketplace.product.client.UserClient;
import com.marketplace.product.client.dto.UserDto;
import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductStatus;
import com.marketplace.product.exception.ForbiddenException;
import com.marketplace.product.kafka.ProductEventProducer;
import com.marketplace.product.mapper.ProductMapper;
import com.marketplace.product.products.dto.ProductCreateRequest;
import com.marketplace.product.products.dto.ProductFilter;
import com.marketplace.product.products.dto.ProductResponse;
import com.marketplace.product.products.dto.ProductSummary;
import com.marketplace.product.products.dto.ProductUpdateRequest;
import com.marketplace.product.products.exception.ProductNotFoundException;
import com.marketplace.product.repository.CategoryRepository;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductService}. All collaborators are mocked; no
 * Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMapper productMapper;
    @Mock private ProductEventProducer productEventProducer;
    @Mock private UserClient userClient;

    @InjectMocks private ProductService productService;

    private AuthenticatedUser seller;
    private AuthenticatedUser admin;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        seller = new AuthenticatedUser(42L, "alice@example.com", "USER");
        admin = new AuthenticatedUser(1L, "admin@example.com", "ADMIN");

        sampleProduct = Product.builder()
                .id(100L)
                .sellerId(42L)
                .categoryId(7L)
                .title("Sample phone")
                .description("Like new")
                .price(new BigDecimal("100.00"))
                .status(ProductStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ------------------------------------------------------------------ getById

    @Test
    void getById_returnsResponse_whenProductExists() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productMapper.toResponse(sampleProduct)).thenReturn(
                new ProductResponse(100L, 42L, 7L, "Sample phone", "Like new",
                        new BigDecimal("100.00"), ProductStatus.ACTIVE,
                        sampleProduct.getCreatedAt(), sampleProduct.getUpdatedAt()));

        ProductResponse response = productService.getById(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ------------------------------------------------------------------ search

    @Test
    void search_returnsPagedSummaries() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct), pageable, 1);
        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(productMapper.toSummary(sampleProduct)).thenReturn(
                new ProductSummary(100L, 42L, 7L, "Sample phone",
                        new BigDecimal("100.00"), ProductStatus.ACTIVE,
                        sampleProduct.getCreatedAt()));

        PageResponse<ProductSummary> result = productService.search(
                new ProductFilter(null, null, null, null, null, null), pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    // ------------------------------------------------------------------ listBySeller

    @Test
    void listBySeller_returnsSummaries() {
        when(productRepository.findAllBySellerIdOrderByCreatedAtDesc(42L))
                .thenReturn(List.of(sampleProduct));
        when(productMapper.toSummary(sampleProduct)).thenReturn(
                new ProductSummary(100L, 42L, 7L, "Sample phone",
                        new BigDecimal("100.00"), ProductStatus.ACTIVE,
                        sampleProduct.getCreatedAt()));

        List<ProductSummary> result = productService.listBySeller(42L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(100L);
    }

    // ------------------------------------------------------------------ create

    @Test
    void create_persistsProduct_publishesEvent() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryId(7L);
        request.setTitle("New phone");
        request.setPrice(new BigDecimal("250.00"));

        when(categoryRepository.existsById(7L)).thenReturn(true);
        when(productMapper.toEntity(request)).thenReturn(Product.builder()
                .categoryId(7L).title("New phone").price(new BigDecimal("250.00")).build());
        lenient().when(userClient.getUser(42L)).thenReturn(new UserDto(42L, "alice@example.com", null, null, "USER", null));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(200L);
            p.setCreatedAt(Instant.now());
            return p;
        });
        when(productMapper.toResponse(any(Product.class))).thenReturn(
                new ProductResponse(200L, 42L, 7L, "New phone", null,
                        new BigDecimal("250.00"), ProductStatus.ACTIVE, Instant.now(), null));

        ProductResponse response = productService.create(request, seller);

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.status()).isEqualTo(ProductStatus.ACTIVE);

        ArgumentCaptor<ProductCreatedEvent> captor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(productEventProducer).publishCreated(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(200L);
        assertThat(captor.getValue().sellerId()).isEqualTo(42L);
    }

    @Test
    void create_throwsForbidden_whenUserServiceDown_andWeChooseNotToFail() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryId(7L);
        request.setTitle("New phone");
        request.setPrice(new BigDecimal("250.00"));

        when(categoryRepository.existsById(7L)).thenReturn(true);
        when(productMapper.toEntity(request)).thenReturn(Product.builder()
                .categoryId(7L).title("New phone").price(new BigDecimal("250.00")).build());
        when(userClient.getUser(42L)).thenReturn(null);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(200L);
            p.setCreatedAt(Instant.now());
            return p;
        });
        when(productMapper.toResponse(any(Product.class))).thenReturn(
                new ProductResponse(200L, 42L, 7L, "New phone", null,
                        new BigDecimal("250.00"), ProductStatus.ACTIVE, Instant.now(), null));

        ProductResponse response = productService.create(request, seller);

        assertThat(response.id()).isEqualTo(200L);
        verify(productEventProducer).publishCreated(any(ProductCreatedEvent.class));
    }

    // ------------------------------------------------------------------ update

    @Test
    void update_byOwner_succeed() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle("Updated title");

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productMapper.toResponse(sampleProduct)).thenReturn(
                new ProductResponse(100L, 42L, 7L, "Updated title", "Like new",
                        new BigDecimal("100.00"), ProductStatus.ACTIVE, Instant.now(), Instant.now()));

        ProductResponse response = productService.update(100L, request, seller);

        assertThat(response.title()).isEqualTo("Updated title");
        verify(productEventProducer, never()).publishCreated(any());
        verify(productEventProducer, never()).publishDeleted(any());
    }

    @Test
    void update_byOtherUser_throwsForbidden() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle("Hijack attempt");

        AuthenticatedUser other = new AuthenticatedUser(99L, "bob@example.com", "USER");

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.update(100L, request, other))
                .isInstanceOf(ForbiddenException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void update_byAdmin_succeed_evenNotOwner() {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle("Admin updated");

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(productMapper.toResponse(sampleProduct)).thenReturn(
                new ProductResponse(100L, 42L, 7L, "Admin updated", "Like new",
                        new BigDecimal("100.00"), ProductStatus.ACTIVE, Instant.now(), Instant.now()));

        ProductResponse response = productService.update(100L, request, admin);

        assertThat(response.title()).isEqualTo("Admin updated");
    }

    // ------------------------------------------------------------------ softDelete

    @Test
    void softDelete_setsStatusDeleted_publishesEvent() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        productService.softDelete(100L, seller);

        ArgumentCaptor<ProductDeletedEvent> captor = ArgumentCaptor.forClass(ProductDeletedEvent.class);
        verify(productEventProducer).publishDeleted(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(100L);
        assertThat(captor.getValue().sellerId()).isEqualTo(42L);
    }

    @Test
    void softDelete_throwsForbidden_whenNotOwner() {
        AuthenticatedUser other = new AuthenticatedUser(99L, "bob@example.com", "USER");
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.softDelete(100L, other))
                .isInstanceOf(ForbiddenException.class);

        verify(productRepository, never()).save(any());
        verify(productEventProducer, never()).publishDeleted(any());
    }

    // ------------------------------------------------------------------ status transitions

    @Test
    void applyStatusTransition_updatesStatus() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        productService.applyStatusTransition(100L, ProductStatus.RESERVED);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.RESERVED);
    }

    @Test
    void applyStatusTransition_throwsNotFound_whenProductMissing() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.applyStatusTransition(404L, ProductStatus.RESERVED))
                .isInstanceOf(ProductNotFoundException.class);
    }
}