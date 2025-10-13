package com.ducnhu.auth.dto;

public record JwtResponse(String tokenType, String accessToken, long expiresInSeconds, String fullName) {}

