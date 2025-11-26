package com.ducnhu.common.security;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    private final CorsProperties props;

    public CorsConfig(CorsProperties props) {
        this.props = props;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        List<String> origins = splitCsv(props.getAllowedOrigins());
        List<String> methods  = splitCsv(props.getAllowedMethods());
        List<String> headers  = splitCsv(props.getAllowedHeaders());

        cfg.setAllowedOrigins(origins);      // ["http://localhost:4200"]
        cfg.setAllowedMethods(methods);      // ["GET", "POST", "PUT", ...]
        cfg.setAllowedHeaders(headers);      // ["Content-Type", "Authorization", ...]
        cfg.setAllowCredentials(props.isAllowCredentials());

        // Cho phép browser đọc các header này từ response
        cfg.setExposedHeaders(List.of("Authorization", "Link", "X-Total-Count"));

        // Cho tất cả path trong service
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);

        return source;
    }

    private static List<String> splitCsv(String s) {
        if (!StringUtils.hasText(s)) {
            return List.of();
        }
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toList());
    }
}
