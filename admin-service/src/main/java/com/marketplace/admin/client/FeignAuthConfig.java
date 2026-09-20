package com.marketplace.admin.client;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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