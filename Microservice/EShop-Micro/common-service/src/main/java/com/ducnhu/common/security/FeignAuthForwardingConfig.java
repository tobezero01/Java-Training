package com.ducnhu.common.security;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthForwardingConfig {
    @Bean
    public RequestInterceptor authForwarder() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String authorization = attributes.getRequest().getHeader("Authorization");
                if (authorization != null && !authorization.isBlank()) {
                    requestTemplate.header("Authorization", authorization);
                    String trace = attributes.getRequest().getHeader("traceparent");
                    if (trace != null) requestTemplate.header("traceparent", trace);
                }

            }
        };

    }
}
