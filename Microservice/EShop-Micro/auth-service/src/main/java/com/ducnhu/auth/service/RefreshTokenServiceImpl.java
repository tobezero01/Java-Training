package com.ducnhu.auth.service;

import com.ducnhu.auth.entity.RefreshToken;
import com.ducnhu.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    private String newOpaqueToken() {
        return RandomStringUtils.randomAlphanumeric(64);
    }

    @Override
    @Transactional
    public RefreshToken issue(Integer customerId, int ttlDays, String ip, String ua) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setCustomerId(customerId);
        refreshToken.setToken(newOpaqueToken());
        refreshToken.setExpiresAt(Instant.now().plus(ttlDays, ChronoUnit.DAYS));
        refreshToken.setCreatedIp(ip);
        refreshToken.setUserAgent(ua);
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findActive(String token) {
        return refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()));
    }

    @Override
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByTokenAndRevokedFalse(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    @Transactional
    public void revokeAllForUser(Integer customerId) {
        refreshTokenRepository.findAll().stream()
                .filter(rt -> rt.getCustomerId().equals(customerId) && !rt.isRevoked())
                .forEach(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }
}
