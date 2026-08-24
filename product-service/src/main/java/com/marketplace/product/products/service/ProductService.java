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
import com.marketplace.product.products.repository.ProductSpecifications;
import com.marketplace.product.repository.CategoryRepository;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.product.catalog.exception.CategoryNotFoundException;
import com.marketplace.product.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.marketplace.product.constant.ExceptionMessages.CATEGORY_NOT_FOUND;
import static com.marketplace.product.constant.ExceptionMessages.NOT_OWNER;
import static com.marketplace.product.constant.ExceptionMessages.PRODUCT_NOT_FOUND;

/**
 * Product CRUD and filtering operations.
 *
 * <p>Seller id comes from the JWT principal on create; ownership checks
 * enforce that updates/deletes are only done by the owner or an admin.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductEventProducer productEventProducer;
    private final UserClient userClient;

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummary> search(ProductFilter filter, Pageable pageable) {
        Specification<Product> spec = Specification
                .where(ProductSpecifications.hasCategory(filter.categoryId()))
                .and(ProductSpecifications.hasSeller(filter.sellerId()))
                .and(ProductSpecifications.priceBetween(filter.minPrice(), filter.maxPrice()))
                .and(ProductSpecifications.withStatus(filter.status()))
                .and(ProductSpecifications.titleContains(filter.search()));

        Page<Product> page = productRepository.findAll(spec, pageable);
        List<ProductSummary> content = page.getContent().stream()
                .map(productMapper::toSummary)
                .toList();

        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ProductSummary> listBySeller(Long sellerId) {
        return productRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(productMapper::toSummary)
                .toList();
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest request, AuthenticatedUser principal) {
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new CategoryNotFoundException(CATEGORY_NOT_FOUND);
        }

        // Best-effort existence check via Feign — fallback returns null
        // when user-service is unreachable, in which case we trust the JWT.
        UserDto seller = userClient.getUser(principal.id());
        if (seller == null) {
            log.warn("user-service unreachable during product creation; trusting JWT claim for sellerId={}",
                    principal.id());
        }

        Product product = productMapper.toEntity(request);
        product.setSellerId(principal.id());
        product.setStatus(ProductStatus.ACTIVE);

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, sellerId={}, title={}",
                saved.getId(), saved.getSellerId(), saved.getTitle());

        productEventProducer.publishCreated(new ProductCreatedEvent(
                saved.getId(),
                saved.getSellerId(),
                saved.getTitle(),
                saved.getPrice(),
                saved.getCategoryId(),
                saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now()
        ));

        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request, AuthenticatedUser principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        ensureOwnerOrAdmin(product, principal);

        productMapper.updateEntity(request, product);
        Product saved = productRepository.save(product);
        log.info("Product updated: id={}", saved.getId());
        return productMapper.toResponse(saved);
    }

    @Transactional
    public void softDelete(Long id, AuthenticatedUser principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        ensureOwnerOrAdmin(product, principal);

        product.setStatus(ProductStatus.DELETED);
        Product saved = productRepository.save(product);
        log.info("Product soft-deleted: id={}", saved.getId());

        productEventProducer.publishDeleted(new ProductDeletedEvent(
                saved.getId(),
                saved.getSellerId(),
                principal.id(),
                "soft-delete",
                Instant.now()
        ));
    }

    /**
     * Status transition helper used by Kafka consumers. Loads the product,
     * updates the status, and returns the new state. Caller owns transaction.
     */
    @Transactional
    public void applyStatusTransition(Long productId, ProductStatus newStatus) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND));
        ProductStatus oldStatus = product.getStatus();
        product.setStatus(newStatus);
        productRepository.save(product);
        log.info("Product status transition: id={}, {} -> {}", productId, oldStatus, newStatus);
    }

    private static void ensureOwnerOrAdmin(Product product, AuthenticatedUser principal) {
        if (principal.isAdmin()) {
            return;
        }
        if (!product.getSellerId().equals(principal.id())) {
            throw new ForbiddenException(NOT_OWNER);
        }
    }
}