package com.redis.demo.redis.service;// src/main/java/com/eshop/infrastructure/RedisCacheService.java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.demo.redis.config.CacheTtl;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class RedisCacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String NULL_MARKER = "__NULL__";
    private static final Duration DEFAULT_NULL_TTL = CacheTtl.NULL_SHORT;

    public RedisCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void putNull(String key, Duration ttl) {
        redisTemplate.opsForValue().set(key, NULL_MARKER, ttl);
    }

    private boolean isNullMarker(String raw) {
        return NULL_MARKER.equals(raw);
    }

    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        if (hasKey(key)) {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null || isNullMarker(raw)) return null;
            try {
                return objectMapper.readValue(raw, type);
            } catch (Exception e) {
                evict(key); // nếu lỗi parse JSON thì xóa key
            }
        }
        T val = loader.get();
        if (val == null) { putNull(key, DEFAULT_NULL_TTL); return null; }
        put(key, val, ttl);
        return val;
    }

    public <T> T getOrLoad(String key, TypeReference<T> ref, Duration ttl, Supplier<T> loader) {
        if (hasKey(key)) {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null || isNullMarker(raw)) return null;
            try {
                return objectMapper.readValue(raw, ref);
            } catch (Exception e) {
                evict(key);
            }
        }
        T val = loader.get();
        if (val == null) { putNull(key, DEFAULT_NULL_TTL); return null; }
        put(key, val, ttl);
        return val;
    }

    public void put(String key, Object value, Duration ttl) {
        if (value == null) {
            putNull(key, DEFAULT_NULL_TTL);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis cache put failed for key " + key, e);
        }
    }

    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        if (value == null) {
            return Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(key, NULL_MARKER, (ttl != null ? ttl : DEFAULT_NULL_TTL)));
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, json, ttl));
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    public <T> Optional<T> getIfPresent(String key, Class<T> type) {
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || isNullMarker(raw)) return Optional.empty();
        try {
            return Optional.ofNullable(objectMapper.readValue(raw, type));
        } catch (Exception e) {
            evict(key);
            return Optional.empty();
        }
    }

    public <T> Optional<T> getIfPresent(String key, TypeReference<T> ref) {
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null || isNullMarker(raw)) return Optional.empty();
        try {
            return Optional.ofNullable(objectMapper.readValue(raw, ref));
        } catch (Exception e) {
            evict(key);
            return Optional.empty();
        }
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }

    public long evictByPrefix(String prefix) {
        List<String> keys = new ArrayList<>();
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            try (var cursor = connection.scan(ScanOptions.scanOptions()
                    .match(prefix + "*").count(1000).build())) {
                cursor.forEachRemaining(item -> keys.add(new String(item)));
            }
            return null;
        });
        if (!keys.isEmpty()) {
            return redisTemplate.delete(keys);
        }
        return 0;
    }
}
