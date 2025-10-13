package com.redis.demo.redis.config;

import com.redis.demo.redis.entity.Product;
/** Tính score double cho ZSET: ưu tiên created_time, tie-break id */
public final class ScoreUtil {
    private ScoreUtil() {}

    public static double scoreByCreated(Product p) {
        long ms = (p.getCreatedTime() != null ? p.getCreatedTime().getTime() : 0L);
        // cộng thêm id * 1e-6 để tránh trùng score, vẫn giữ tính ổn định
        return ms + (p.getId() != null ? (p.getId() % 1_000_000) / 1_000_000.0 : 0.0);
    }
}
