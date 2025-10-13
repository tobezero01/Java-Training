package com.ducnhu.auth.dto;

public record LoginRequest(
        String email, String password,
        Boolean rememberMe
) {
}
