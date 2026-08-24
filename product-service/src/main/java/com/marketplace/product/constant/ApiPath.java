package com.marketplace.product.constant;

/**
 * API path constants for the product-service.
 *
 * <p>All controllers reference these constants via {@code import static}
 * to keep URLs consistent and refactor-safe.</p>
 */
public final class ApiPath {

    private ApiPath() {
        // Utility class — no instances.
    }

    public static final String CATEGORIES_BASE = "/api/categories";

    public static final String PRODUCTS_BASE = "/api/products";
    public static final String PRODUCTS_SELLER = "/seller/{userId}";
    public static final String PRODUCTS_IMAGES = "/{id}/images";

    public static final String UPLOADS_BASE = "/uploads";
}