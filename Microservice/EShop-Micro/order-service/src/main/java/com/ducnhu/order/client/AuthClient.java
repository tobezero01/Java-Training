package com.ducnhu.order.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service", url = "${AUTH_BASE:http://gateway:8080}")
public interface AuthClient {
    @GetMapping("/api/auth/me")
    MeResponse me(@RequestHeader("Authorization") String authHeader);
}
