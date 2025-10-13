package com.ducnhu.catalog.service;

import com.ducnhu.common.cache.CacheTtl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.ducnhu.common.cache.CacheKey;
/**
 * - Bitmap tồn tại ID: 1 bit/id -> GETBIT/SETBIT O(1), RAM cực thấp
 * - alias2id: String key -> parse id nhanh rồi dùng lại cache prod:id:{id}
 * - Có cache âm (null-marker) cho alias không tồn tại
 */
@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private final StringRedisTemplate redis;

    private static final String NULL_MARKER = "__NULL__";

    // ====== Bitmap ID ======

    /** id có thể tồn tại? (bit=1) -> true; (bit=0) -> false */
    public boolean idMayExist(int id) {
        // GETBIT key offset
        Boolean bit = redis.opsForValue().getBit(CacheKey.prodIdBitmap(), id);
        return Boolean.TRUE.equals(bit);
    }

    /** đánh dấu id tồn tại (sau khi thấy trong DB) */
    public void markIdExists(int id) {
        redis.opsForValue().setBit(CacheKey.prodIdBitmap(), id, true);
    }

    /** (tuỳ chọn) xoá bit khi chắc chắn đã delete trong DB */
    public void clearId(int id) {
        redis.opsForValue().setBit(CacheKey.prodIdBitmap(), id, false);
    }

    // ====== Alias -> ID ======

    /** tra alias -> id (null nếu không có hoặc là null-marker) */
    public Integer resolveAlias(String alias) {
        String v = redis.opsForValue().get(CacheKey.prodAlias2Id(alias));
        if (v == null || NULL_MARKER.equals(v)) return null;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    /** set alias -> id (TTL dài) */
    public void setAliasToId(String alias, Integer id) {
        if (id == null) return;
        redis.opsForValue().set(CacheKey.prodAlias2Id(alias), id.toString(), CacheTtl.ALIAS_TO_ID);
    }

    /** cache âm alias không tồn tại (TTL ngắn) */
    public void setAliasNull(String alias) {
        redis.opsForValue().set(CacheKey.prodAlias2Id(alias), NULL_MARKER, CacheTtl.NULL_SHORT);
    }
}
