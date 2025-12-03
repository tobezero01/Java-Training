package com.ducnhu.cart;

import feign.RequestInterceptor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Configuration
public class FeignConfig {
    private static final String ACCESS_TOKEN_COOKIE_NAME = "eshop_access_token";

    @Bean
    public RequestInterceptor authRelay() {
        return template -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();

                // 1. Logic mới: Tìm token trong Cookie và gửi đi dạng Cookie
                // Auth Service (JwtAuthenticationFilter) sẽ đọc được vì nó hỗ trợ đọc Cookie
                if (request.getCookies() != null) {
                    for (Cookie cookie : request.getCookies()) {
                        if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                            // Gắn header Cookie vào request gửi đi
                            // Format header Cookie: "name=value"
                            template.header("Cookie", cookie.getName() + "=" + cookie.getValue());
                            break;
                        }
                    }
                }

                // 2. Logic cũ: Fallback tìm header Authorization (cho Mobile/Postman cũ)
                String auth = request.getHeader("Authorization");
                if (auth != null && !auth.isBlank()) {
                    template.header("Authorization", auth);
                }

                // 3. Giữ nguyên Trace log
                String trace = request.getHeader("traceparent");
                if (trace != null) {
                    template.header("traceparent", trace);
                }
            }
        };
    }
}