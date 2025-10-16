package com.ducnhu.checkout.service;

import com.ducnhu.checkout.dto.MeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name="auth-service", path="/api/auth")
public interface AuthClient {
    @GetMapping("/me")
    MeResponse me();
}
