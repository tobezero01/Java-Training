package com.ducnhu.auth.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {
    @Value("${spring.security.jwt.secret}")
    private String secret;
    @Value("${spring.security.jwt.issuer}")
    private String issuer;
    @Value("${spring.security.jwt.access-ttl-seconds}")
    private long ttlSeconds;

    private Key signingKey() {
        byte[] bytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(bytes);
    }

    @Override
    public String generateAccessToken(UserDetails user, Map<String, Object> claims) {
        Instant now = Instant.now(), exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .setClaims(new HashMap<>(claims))
                .setSubject(user.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .setIssuer(issuer)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    @Override
    public boolean isTokenValid(String token, UserDetails user) {
        try {
            var c = Jwts.parserBuilder().setSigningKey(signingKey()).build()
                    .parseClaimsJws(token).getBody();
            return user.getUsername().equals(c.getSubject()) && c.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public long getAccessTokenTtlSeconds() {
        return ttlSeconds;
    }
}