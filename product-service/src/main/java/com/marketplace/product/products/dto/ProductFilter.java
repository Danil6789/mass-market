package com.marketplace.product.products.dto;

import com.marketplace.product.entity.ProductStatus;

import java.math.BigDecimal;

/**
 * Filter parameters for {@code GET /api/products}. Each field is optional —
 * {@code null} means "don't filter on this dimension".
 *
 * @param categoryId  filter by category
 * @param minPrice    lower price bound (inclusive)
 * @param maxPrice    upper price bound (inclusive)
 * @param status      filter by product status
 * @param search      case-insensitive substring match against {@code title}
 * @param sellerId    filter by seller
 */
public record ProductFilter(
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ProductStatus status,
        String search,
        Long sellerId
) {
}