package com.ducnhu.auth.service;

import com.ducnhu.auth.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    JwtResponse login(LoginRequest request,
                      HttpServletRequest httpRequest,
                      HttpServletResponse httpResponse);

    JwtResponse refresh(HttpServletRequest httpRequest,
                        HttpServletResponse httpResponse);

    void logout(HttpServletRequest httpRequest,
                HttpServletResponse httpResponse);

    MeResponse me();

    SimpleMessageResponse register(RegisterRequest request,
                                   HttpServletRequest httpRequest);

    VerifyResponse verify(String code);

    SimpleMessageResponse forgotPassword(ForgotPasswordRequest request,
                                         HttpServletRequest httpRequest);

    SimpleMessageResponse resetPassword(ResetPasswordRequest request);
}
