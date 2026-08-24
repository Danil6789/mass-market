package com.marketplace.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for the product-service admin endpoints. Admin-service
 * uses this to delete products without holding the product row in its own DB.
 */
@FeignClient(
        name = "product-service",
        url = "${app.clients.product-service:http://localhost:8082}",
        fallbackFactory = FallbackProductClient.class
)
public interface ProductClient {

    @DeleteMapping("/api/products/{id}")
    void deleteProduct(@PathVariable("id") Long id);
}
