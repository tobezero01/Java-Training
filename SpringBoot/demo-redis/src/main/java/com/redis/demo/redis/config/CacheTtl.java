package com.redis.demo.redis.config;

import java.time.Duration;

public final class CacheTtl {
    public static final Duration SHORT = Duration.ofMinutes(10);
    public static final Duration MEDIUM = Duration.ofHours(1);
    public static final Duration LONG = Duration.ofHours(12);

    // TTL cho null marker để tránh cache null quá lâu
    public static final Duration NULL_SHORT = Duration.ofMinutes(2);

    private CacheTtl() {}
}
