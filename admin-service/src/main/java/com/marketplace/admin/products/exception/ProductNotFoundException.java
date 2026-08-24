package com.marketplace.admin.products.exception;

/**
 * Thrown when admin tries to delete a product that does not exist (404 from
 * product-service) or when the Feign client reports a missing product.
 */
public class ProductNotFoundException extends RuntimeException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("Product not found: id=" + productId);
        this.productId = productId;
    }

    public static ProductNotFoundException forId(Long id) {
        return new ProductNotFoundException(id);
    }

    public Long getProductId() {
        return productId;
    }
}
