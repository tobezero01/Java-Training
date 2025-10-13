package com.ducnhu.customer.client;

import com.ducnhu.customer.dto.MeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "auth-service")
public interface AuthClient {
    @GetMapping("/api/auth/me")
    MeResponse me();
}