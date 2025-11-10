package com.ducnhu.common.helper;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;


import java.nio.charset.StandardCharsets;
import java.security.Key;

public final class JwtClaimUtils {
    private static volatile Key KEY;
    private static volatile String ISSUER;

    private JwtClaimUtils() {}

    /** Được gọi 1 lần lúc khởi động (xem JwtSupportAutoConfig bên dưới) */
    static void init(String issuer, String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        KEY = Keys.hmacShaKeyFor(keyBytes);
        ISSUER = issuer;
    }

    /** Lấy claim từ token (hỗ trợ "Bearer ...") và convert về kiểu yêu cầu */
    public static <T> T get(String token, String claimName, Class<T> type) {
        if (token == null || token.isBlank()) return null;
        String raw = token.startsWith("Bearer ") ? token.substring(7) : token;
        Claims claims = Jwts.parserBuilder()
                .requireIssuer(ISSUER)
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(raw)
                .getBody();
        Object value = claims.get(claimName);
        return convert(value, type);
    }

    /** Lấy trực tiếp từ request header Authorization */
    public static <T> T get(HttpServletRequest req, String claimName, Class<T> type) {
        String header = req.getHeader("Authorization");
        if (header == null) return null;
        return get(header, claimName, type);
    }

    @SuppressWarnings("unchecked")
    private static <T> T convert(Object v, Class<T> type) {
        if (v == null) return null;
        if (type.isInstance(v)) return type.cast(v);

        if (Number.class.isAssignableFrom(type) && v instanceof Number n) {
            if (type == Integer.class) return (T) Integer.valueOf(n.intValue());
            if (type == Long.class)    return (T) Long.valueOf(n.longValue());
            if (type == Float.class)   return (T) Float.valueOf(n.floatValue());
            if (type == Double.class)  return (T) Double.valueOf(n.doubleValue());
        }
        if (type == String.class)  return (T) String.valueOf(v);
        if (type == Boolean.class) {
            if (v instanceof Boolean b) return (T) b;
            return (T) Boolean.valueOf(String.valueOf(v));
        }
        // fallback—có thể mở rộng convert Map -> POJO nếu cần
        throw new IllegalArgumentException("Cannot convert claim '" + v + "' to " + type.getSimpleName());
    }
}
