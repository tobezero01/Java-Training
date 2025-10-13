package com.ducnhu.auth.dto;

public record ResetPasswordRequest(String token, String newPassword) {}

