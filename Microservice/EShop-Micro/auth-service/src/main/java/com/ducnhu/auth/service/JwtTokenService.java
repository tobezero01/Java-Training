package com.ducnhu.auth.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

public interface JwtTokenService {
    String extractUsername(String token);
    boolean isTokenValid(String token, UserDetails user);
    long getAccessTokenTtlSeconds();
    String generateAccessToken(UserDetails user, Map<String,Object> extraClaims);
}
