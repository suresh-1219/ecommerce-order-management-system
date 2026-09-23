package com.suresh.ecommerce.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI ecommerceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce Order Management System API")
                        .description("Backend REST APIs for product catalog, cart, order placement, "
                                + "Razorpay payments, and JWT-based authentication.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Suresh Kosana")))
                // Register a reusable "bearerAuth" security scheme so protected endpoints
                // show a padlock icon and the Swagger UI "Authorize" button accepts a JWT.
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Applies the bearer scheme as the default requirement for every endpoint;
                // endpoints marked @SecurityRequirements(value = {}) (public ones) override this.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }
}
