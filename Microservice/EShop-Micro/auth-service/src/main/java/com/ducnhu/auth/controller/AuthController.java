package com.ducnhu.auth.controller;


import com.ducnhu.auth.dto.*;
import com.ducnhu.auth.service.AuthService;
import com.ducnhu.auth.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CustomerService customerProfileService;


    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody @Valid LoginRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(customerProfileService.getCurrentProfile());
    }

    @PatchMapping("/me")
    public ResponseEntity<ProfileResponse> updateMe(@Valid @RequestBody ProfileUpdateRequest req) {
        return ResponseEntity.ok(customerProfileService.updateCurrentProfile(req));
    }
    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        return ResponseEntity.ok(authService.refresh(req, res));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
        authService.logout(req, res);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<SimpleMessageResponse> register(@RequestBody @Valid RegisterRequest request,
                                                          HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.register(request, httpRequest));
    }

    @GetMapping("/verify")
    public ResponseEntity<VerifyResponse> verify(@RequestParam("code") String code) {
        return ResponseEntity.ok(authService.verify(code));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<SimpleMessageResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request,
                                                                HttpServletRequest httpReq) {
        return ResponseEntity.ok(authService.forgotPassword(request, httpReq));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<SimpleMessageResponse> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
