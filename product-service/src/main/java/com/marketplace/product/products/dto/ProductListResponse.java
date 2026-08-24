package com.marketplace.product.products.dto;

import com.marketplace.common.dto.PageResponse;

/**
 * Convenience alias for a paginated list of {@link ProductSummary}.
 * Wraps Spring's {@link PageResponse} so the controller signature stays
 * readable.
 */
public record ProductListResponse(
        PageResponse<ProductSummary> page
) {
}