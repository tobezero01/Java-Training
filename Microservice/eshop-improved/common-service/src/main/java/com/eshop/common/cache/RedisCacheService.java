package com.eshop.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Production-grade Redis Cache Service
 * 
 * Features:
 * 1. Cache-aside pattern with loader
 * 2. NULL marker to prevent cache penetration
 * 3. Distributed lock to prevent cache stampede
 * 4. TTL jitter to prevent synchronized expiration
 * 5. Batch operations for efficiency
 * 6. Metrics-ready
 */
@Service
@Slf4j
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    
    @Getter
    private final ObjectMapper objectMapper;

    private static final String NULL_MARKER = "__NULL__";
    private static final Duration DEFAULT_NULL_TTL = Duration.ofSeconds(60);
    private static final Duration DEFAULT_LOCK_TTL = Duration.ofSeconds(10);
    private static final int DEFAULT_TTL_JITTER_PERCENT = 10; // ±10% jitter

    // Lua script for distributed lock (atomic set-if-not-exists with expiry)
    private static final String LOCK_SCRIPT = """
        if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then
            redis.call('pexpire', KEYS[1], ARGV[2])
            return 1
        else
            return 0
        end
        """;

    // Lua script for unlock (only delete if value matches)
    private static final String UNLOCK_SCRIPT = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
        """;

    public RedisCacheService(
            RedisTemplate<String, Object> redisTemplate,
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    // ==================== BASIC OPERATIONS ====================

    /**
     * Get value from cache
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (raw == null) {
                return Optional.empty();
            }
            if (NULL_MARKER.equals(raw)) {
                return Optional.empty(); // Cached null
            }
            return Optional.of(objectMapper.readValue(raw, type));
        } catch (Exception e) {
            log.warn("Cache get failed for key={}: {}", key, e.getMessage());
            evict(key);
            return Optional.empty();
        }
    }

    /**
     * Get value with TypeReference (for generic types like List<T>)
     */
    public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
        try {
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (raw == null || NULL_MARKER.equals(raw)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(raw, typeRef));
        } catch (Exception e) {
            log.warn("Cache get failed for key={}: {}", key, e.getMessage());
            evict(key);
            return Optional.empty();
        }
    }

    /**
     * Put value into cache with TTL
     */
    public void put(String key, Object value, Duration ttl) {
        try {
            if (value == null) {
                putNull(key, DEFAULT_NULL_TTL);
                return;
            }
            String json = objectMapper.writeValueAsString(value);
            Duration jitteredTtl = addJitter(ttl, DEFAULT_TTL_JITTER_PERCENT);
            stringRedisTemplate.opsForValue().set(key, json, jitteredTtl);
        } catch (JsonProcessingException e) {
            log.error("Cache put serialization failed for key={}: {}", key, e.getMessage());
        }
    }

    /**
     * Put NULL marker to prevent cache penetration
     */
    public void putNull(String key, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, NULL_MARKER, ttl);
    }

    /**
     * Put if absent (atomic)
     */
    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        try {
            String json = value == null ? NULL_MARKER : objectMapper.writeValueAsString(value);
            Duration jitteredTtl = addJitter(ttl, DEFAULT_TTL_JITTER_PERCENT);
            return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(key, json, jitteredTtl)
            );
        } catch (JsonProcessingException e) {
            log.error("Cache putIfAbsent failed for key={}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Evict (delete) cache entry
     */
    public void evict(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * Evict multiple keys
     */
    public void evictAll(Collection<String> keys) {
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * Evict by pattern (use sparingly - expensive operation)
     */
    public void evictByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.info("Evicted {} keys matching pattern: {}", keys.size(), pattern);
        }
    }

    /**
     * Check if key exists
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    // ==================== CACHE-ASIDE WITH LOADER ====================

    /**
     * Get from cache or load from source
     * With distributed lock to prevent cache stampede
     */
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        // Try cache first
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Check for NULL marker (cached null)
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (NULL_MARKER.equals(raw)) {
            return null;
        }

        // Cache miss - acquire lock and load
        return loadWithLock(key, type, ttl, loader);
    }

    /**
     * Get or load with TypeReference
     */
    public <T> T getOrLoad(String key, TypeReference<T> typeRef, Duration ttl, Supplier<T> loader) {
        Optional<T> cached = get(key, typeRef);
        if (cached.isPresent()) {
            return cached.get();
        }

        String raw = stringRedisTemplate.opsForValue().get(key);
        if (NULL_MARKER.equals(raw)) {
            return null;
        }

        return loadWithLockGeneric(key, typeRef, ttl, loader);
    }

    /**
     * Load with distributed lock to prevent stampede
     */
    private <T> T loadWithLock(String key, Class<T> type, Duration ttl, Supplier<T> loader) {
        String lockKey = "lock:" + key;
        String lockValue = UUID.randomUUID().toString();

        try {
            // Try to acquire lock
            if (tryLock(lockKey, lockValue, DEFAULT_LOCK_TTL)) {
                try {
                    // Double-check cache after acquiring lock
                    Optional<T> cached = get(key, type);
                    if (cached.isPresent()) {
                        return cached.get();
                    }

                    // Load from source
                    T value = loader.get();
                    
                    // Cache the result (including null)
                    if (value == null) {
                        putNull(key, DEFAULT_NULL_TTL);
                    } else {
                        put(key, value, ttl);
                    }
                    
                    return value;
                } finally {
                    unlock(lockKey, lockValue);
                }
            } else {
                // Failed to acquire lock - wait and retry from cache
                Thread.sleep(50);
                return get(key, type).orElseGet(loader);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        }
    }

    private <T> T loadWithLockGeneric(String key, TypeReference<T> typeRef, Duration ttl, Supplier<T> loader) {
        String lockKey = "lock:" + key;
        String lockValue = UUID.randomUUID().toString();

        try {
            if (tryLock(lockKey, lockValue, DEFAULT_LOCK_TTL)) {
                try {
                    Optional<T> cached = get(key, typeRef);
                    if (cached.isPresent()) {
                        return cached.get();
                    }

                    T value = loader.get();
                    if (value == null) {
                        putNull(key, DEFAULT_NULL_TTL);
                    } else {
                        put(key, value, ttl);
                    }
                    return value;
                } finally {
                    unlock(lockKey, lockValue);
                }
            } else {
                Thread.sleep(50);
                return get(key, typeRef).orElseGet(loader);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        }
    }

    // ==================== DISTRIBUTED LOCK ====================

    /**
     * Try to acquire distributed lock
     */
    public boolean tryLock(String lockKey, String lockValue, Duration ttl) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LOCK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(
            script,
            Collections.singletonList(lockKey),
            lockValue,
            String.valueOf(ttl.toMillis())
        );
        return result != null && result == 1L;
    }

    /**
     * Release distributed lock (only if we own it)
     */
    public boolean unlock(String lockKey, String lockValue) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(
            script,
            Collections.singletonList(lockKey),
            lockValue
        );
        return result != null && result == 1L;
    }

    /**
     * Execute with lock (auto-release)
     */
    public <T> Optional<T> executeWithLock(String lockKey, Duration lockTtl, Supplier<T> action) {
        String lockValue = UUID.randomUUID().toString();
        
        if (tryLock(lockKey, lockValue, lockTtl)) {
            try {
                return Optional.ofNullable(action.get());
            } finally {
                unlock(lockKey, lockValue);
            }
        }
        
        return Optional.empty();
    }

    // ==================== BATCH OPERATIONS ====================

    /**
     * Multi-get (batch read)
     */
    public List<String> multiGet(Collection<String> keys) {
        return stringRedisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * Multi-get and deserialize
     */
    public <T> Map<String, T> multiGet(Collection<String> keys, Class<T> type) {
        List<String> values = multiGet(new ArrayList<>(keys));
        Map<String, T> result = new HashMap<>();
        
        int i = 0;
        for (String key : keys) {
            String raw = values.get(i++);
            if (raw != null && !NULL_MARKER.equals(raw)) {
                try {
                    result.put(key, objectMapper.readValue(raw, type));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to deserialize value for key={}", key);
                }
            }
        }
        
        return result;
    }

    /**
     * Multi-put (batch write)
     */
    public void multiPut(Map<String, Object> entries, Duration ttl) {
        entries.forEach((key, value) -> put(key, value, ttl));
    }

    // ==================== ATOMIC COUNTERS ====================

    /**
     * Increment counter
     */
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * Increment counter with TTL (only set TTL on first increment)
     */
    public Long incrementWithTtl(String key, Duration ttl) {
        Long value = stringRedisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L) {
            stringRedisTemplate.expire(key, ttl);
        }
        return value;
    }

    /**
     * Decrement counter
     */
    public Long decrement(String key) {
        return stringRedisTemplate.opsForValue().decrement(key);
    }

    // ==================== TTL UTILITIES ====================

    /**
     * Get remaining TTL
     */
    public Duration getTtl(String key) {
        Long seconds = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return seconds != null && seconds > 0 ? Duration.ofSeconds(seconds) : Duration.ZERO;
    }

    /**
     * Extend TTL
     */
    public boolean extendTtl(String key, Duration ttl) {
        return Boolean.TRUE.equals(stringRedisTemplate.expire(key, ttl));
    }

    /**
     * Add random jitter to TTL to prevent synchronized expiration
     */
    private Duration addJitter(Duration baseTtl, int jitterPercent) {
        long baseMillis = baseTtl.toMillis();
        long jitterRange = baseMillis * jitterPercent / 100;
        long jitter = ThreadLocalRandom.current().nextLong(-jitterRange, jitterRange + 1);
        return Duration.ofMillis(Math.max(1000, baseMillis + jitter)); // Min 1 second
    }
}
