package com.marketplace.product.client;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign configuration that forwards the incoming {@code Authorization} header
 * to downstream service-to-service calls.
 *
 * <p>When a request hits product-service through api-gateway, the gateway
 * validates the JWT and propagates the {@code Authorization} header.
 * product-service then needs to forward that same header when calling
 * user-service (or other services) via OpenFeign — otherwise the downstream
 * service returns 403 because it sees no credentials.</p>
 */
@Configuration
public class FeignAuthConfig {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    @Bean
    public RequestInterceptor authHeaderInterceptor() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest request = attrs.getRequest();
            String header = request.getHeader(HEADER);
            if (header != null && header.startsWith(PREFIX)) {
                template.header(HEADER, header);
            }
        };
    }
}