package com.ducnhu.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {
    @Bean
    RouteLocator routeLocator(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
                // AUTH (module này đã hoàn thiện trong hướng dẫn)
                .route("auth", r -> r.path("/api/auth/**").uri("lb://auth-service"))

                // CATALOG (ví dụ)
                .route("products", r -> r.path("/api/products/**").uri("lb://catalog-service"))
                .route("categories", r -> r.path("/api/categories/**").uri("lb://catalog-service"))

                // CART
                .route("cart", r -> r.path("/api/cart/**").uri("lb://cart-service"))

                // CHECKOUT / PAYMENT
                .route("checkout", r -> r.path("/api/checkout/**").uri("lb://checkout-service"))
                .route("payments", r -> r.path("/api/payments/**").uri("lb://payment-service"))

                // ORDERS / RETURNS
                .route("orders", r -> r.path("/api/orders/**").uri("lb://order-service"))

                // ADDRESS BOOK
                .route("addresses", r -> r.path("/api/addresses/**").uri("lb://customer-service"))

                // REVIEWS
                .route("reviews", r -> r.path("/api/reviews/**").uri("lb://review-service"))

                // GEO
                .route("geo", r -> r.path("/api/geo/**").uri("lb://geo-service"))

                .build();
    }

}
