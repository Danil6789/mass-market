package com.marketplace.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Image-storage configuration. Bound to {@code app.storage.*} in
 * {@code application.yml}.
 */
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * Filesystem directory where uploaded images are written. Trailing
     * separators are normalised by Spring's resource handler.
     */
    private String basePath = "/var/lib/marketplace/uploads/products";

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}