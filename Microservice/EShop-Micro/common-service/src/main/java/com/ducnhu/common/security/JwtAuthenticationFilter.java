package com.ducnhu.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.Key;
import java.util.List;
import java.util.Map;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final Key key;
    private final String issuer;

    // Tên cookie phải khớp với tên Backend đã set (eshop_access_token)
    private static final String ACCESS_TOKEN_COOKIE_NAME = "eshop_access_token";

    public JwtAuthenticationFilter(JwtProperties props) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(props.getSecret());
        } catch (IllegalArgumentException ex) {
            keyBytes = props.getSecret().getBytes();
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = props.getIssuer();
    }

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

        // 2. Nếu không có trong Cookie, tìm trong Header (Fallback cho Mobile/Postman)
        if (token == null) {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }
        }

        // 3. Nếu tìm thấy token, tiến hành xác thực
        if (token != null) {
            try {
                // Parse và Validate Token
                Jws<Claims> jws = Jwts.parserBuilder()
                        .requireIssuer(issuer)
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token);

                String username = jws.getBody().getSubject(); // email

                // Nếu token hợp lệ và chưa có Authentication trong Context
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Lưu ý: Ở đây bạn đang set Authorities là rỗng (List.of()).
                    // Nếu token có chứa roles, hãy extract nó từ jws.getBody().get("roles")
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(username, null, List.of());

                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Token lỗi hoặc hết hạn -> Trả về 401 JSON ngay lập tức và DỪNG chain
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                new ObjectMapper().writeValue(res.getOutputStream(),
                        Map.of("error", "UNAUTHORIZED", "message", "Invalid or Expired JWT"));
                return; // Quan trọng: return để không chạy tiếp filter chain
            }
        }

        // 4. Token null hoặc xác thực thành công -> Đi tiếp
        chain.doFilter(req, res);
    }
}