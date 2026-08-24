package com.marketplace.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Exposes the {@code /uploads/**} resource handler so that uploaded images
 * can be served directly by Spring without a separate reverse-proxy hop.
 *
 * <p>The actual files live under {@code app.storage.base-path}; see
 * {@link StorageProperties}.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    public WebConfig(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + storageProperties.getBasePath() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}