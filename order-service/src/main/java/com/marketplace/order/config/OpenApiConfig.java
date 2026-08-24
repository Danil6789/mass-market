package com.marketplace.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Top-level SpringDoc configuration: registers the JWT bearer scheme so the
 * "Authorize" button works in Swagger UI and every protected endpoint is
 * automatically marked as secured.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI marketplaceOrderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Marketplace · Order Service API")
                        .description("Жизненный цикл заказов и сага через Kafka.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Marketplace Platform")
                                .email("dev@marketplace.local"))
                        .license(new License()
                                .name("Internal")
                                .url("https://marketplace.local")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT access token issued by /api/auth/login (user-service)")));
    }
}
