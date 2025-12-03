package com.ducnhu.auth.security;

import com.ducnhu.auth.service.JwtTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final CustomerUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    // Tên Cookie phải khớp với bên AuthServiceImpl đã lưu
    private static final String ACCESS_TOKEN_COOKIE_NAME = "eshop_access_token";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String token = null;

        // 1. Ưu tiên lấy Token từ HttpOnly Cookie
        if (req.getCookies() != null) {
            for (Cookie cookie : req.getCookies()) {
                if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // 2. Nếu không có Cookie, thử lấy từ Header (Fallback cho Mobile/Postman)
        if (token == null) {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }
        }

        // 3. Nếu tìm thấy Token (từ Cookie hoặc Header), tiến hành xác thực
        if (token != null && !token.isBlank()) {
            try {
                String username = jwtTokenService.extractUsername(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails user = userDetailsService.loadUserByUsername(username);

                    if (jwtTokenService.isTokenValid(token, user)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Token không hợp lệ hoặc hết hạn -> Trả về lỗi 401 ngay lập tức
                // Lưu ý: Nếu muốn filter này "mềm mỏng" hơn (cho phép request đi tiếp dù token sai - để controller xử lý),
                // thì bỏ dòng authenticationEntryPoint.commence đi và chỉ log lỗi.
                log.error("Invalid JWT Token: {}", e.getMessage());
                authenticationEntryPoint.commence(req, res,
                        new AuthenticationException("Invalid JWT", e) {});
                return; // Dừng filter chain
            }
        }

        chain.doFilter(req, res);
    }
}