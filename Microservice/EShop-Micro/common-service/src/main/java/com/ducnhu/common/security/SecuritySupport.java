package com.ducnhu.common.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SecuritySupport {

    @Bean
    public SecurityFilterChain resourceChain(HttpSecurity http, JwtProperties props) throws Exception {
        http
                .cors(c -> {})
                .csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()

                        // ⬇️ TẠM THỜI mở capture để khoanh vùng — chỉ dùng khi debug
                        //.requestMatchers(HttpMethod.POST, "/api/payments/paypal/capture").permitAll()

                        // Các GET public
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/home/**", "/api/geo/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    String msg = ex != null ? ex.getMessage() : "Unauthorized";
                    res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + msg + "\"}");
                }));

        http.addFilterBefore(new JwtAuthenticationFilter(props), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
