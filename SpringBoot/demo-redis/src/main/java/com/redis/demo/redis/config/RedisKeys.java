package com.redis.demo.redis.config;

public final class RedisKeys {
    private RedisKeys() {}

    // Index ZSET theo danh mục + sort=created (có thể mở rộng thêm sort khác)
    public static String idxCatCreated(int catId) {
        return "idx:cat:" + catId + ":created";
    }

    // Cache chi tiết sp (đang có)
    public static String productKey(int id) {
        return "product:" + id;
    }

    // (tuỳ chọn) Version category
    public static String catVersionHash() {
        return "cat:ver";
    }
}
