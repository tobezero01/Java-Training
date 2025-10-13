package com.ducnhu.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecuritySupport {

    @Bean
    public SecurityFilterChain resourceChain(HttpSecurity http, JwtProperties props) throws Exception {
        http.cors(c -> {}).csrf(c -> c.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products/**","/api/categories/**","/api/home/**","/api/geo/**").permitAll()
                        .anyRequest().authenticated()
                );
        http.addFilterBefore(new JwtAuthenticationFilter(props), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
