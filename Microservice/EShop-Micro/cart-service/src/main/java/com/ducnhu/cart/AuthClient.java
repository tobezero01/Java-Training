package com.ducnhu.cart;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "auth-service")
public interface AuthClient {
    @GetMapping("/api/auth/me")
    MeResponse me();
}
