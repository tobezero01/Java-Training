package com.ducnhu.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 1. Cho phép tất cả các domain (hoặc chỉ định cụ thể như "http://localhost:4200")
        // Dùng addAllowedOriginPattern("*") thay vì addAllowedOrigin("*") để dùng được với setAllowCredentials(true)
        corsConfig.addAllowedOriginPattern("*");

        // 2. Cho phép tất cả các method (GET, POST, PUT, DELETE, OPTIONS...)
        corsConfig.addAllowedMethod("*");

        // 3. Cho phép tất cả các header (Authorization, Content-Type...)
        corsConfig.addAllowedHeader("*");

        // 4. Cho phép gửi cookie/credential (quan trọng nếu bạn dùng Token trong cookie hoặc header)
        corsConfig.setAllowCredentials(true);

        // 5. Thời gian cache cấu hình pre-flight (tính bằng giây)
        corsConfig.setMaxAge(3600L);

        // Đăng ký cấu hình này cho tất cả các đường dẫn (/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}