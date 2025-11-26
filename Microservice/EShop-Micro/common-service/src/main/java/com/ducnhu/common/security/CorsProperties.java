package com.ducnhu.common.security;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    /**
     * Ví dụ: "http://localhost:4200"
     */
    private String allowedOrigins;

    /**
     * Ví dụ: "GET,POST,PUT,DELETE,PATCH,OPTIONS"
     */
    private String allowedMethods;

    /**
     * Ví dụ: "Content-Type,Authorization,X-Requested-With"
     */
    private String allowedHeaders;

    /**
     * allow-credentials: true/false
     */
    private boolean allowCredentials;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}