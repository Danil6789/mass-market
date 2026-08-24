package com.marketplace.product.products.repository;

import com.marketplace.product.entity.Product;
import com.marketplace.product.entity.ProductStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Static factory methods for building Spring Data JPA {@link Specification}s
 * over {@link Product}. Each method returns a partial specification that can be
 * chained with {@code Specification.where(...).and(...)} — null inputs are
 * ignored so empty filter objects produce an "all rows" specification.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
        // Utility class — no instances.
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    public static Specification<Product> hasSeller(Long sellerId) {
        if (sellerId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("sellerId"), sellerId);
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> withStatus(ProductStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> titleContains(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }
}