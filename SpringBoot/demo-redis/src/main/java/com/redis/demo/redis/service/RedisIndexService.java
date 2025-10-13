package com.redis.demo.redis.service;

import com.redis.demo.redis.config.RedisKeys;
import com.redis.demo.redis.config.ScoreUtil;
import com.redis.demo.redis.entity.Product;
import com.redis.demo.redis.repo.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Quản lý INDEX IDs trong Redis (ZSET), 1 ZSET / category / sort (created).
 * - build/rebuild index theo batch
 * - addOne/updateOne/removeOne khi sp thay đổi
 * - read page IDs từ ZSET
 */
@Service
public class RedisIndexService {

    private final StringRedisTemplate redis;
    private final ProductRepository repo;

    public RedisIndexService(StringRedisTemplate redis, ProductRepository repo) {
        this.redis = redis;
        this.repo = repo;
    }

    /** Lấy danh sách ID theo trang từ ZSET (page-based pagination) */
    public List<Integer> getPageIdsByCategory(int catId, int page, int size) {
        String key = RedisKeys.idxCatCreated(catId);

        // start/stop theo 0-based index (page>=1). Dùng ZREVRANGE để mới nhất trước.
        long start = (long) (page - 1) * size;
        long stop  = start + size - 1;

        // ZREVRANGE trả list string -> convert Integer
        List<String> members = redis.opsForZSet().reverseRange(key, start, stop)
                .stream().toList();

        if (members.isEmpty()) return List.of();
        return members.stream().map(Integer::valueOf).toList();
    }

    /** Thêm/cập nhật 1 sản phẩm vào INDEX theo category */
    public void upsertOne(Product p) {
        if (p.getCategory() == null) return;
        String key = RedisKeys.idxCatCreated(p.getCategory().getId());
        double score = ScoreUtil.scoreByCreated(p);
        redis.opsForZSet().add(key, String.valueOf(p.getId()), score);
    }

    /** Xoá 1 sản phẩm khỏi INDEX (khi disabled/đổi category) */
    public void removeOne(Integer id, Integer oldCatId) {
        if (oldCatId == null) return;
        String key = RedisKeys.idxCatCreated(oldCatId);
        redis.opsForZSet().remove(key, String.valueOf(id));
    }

    /** Rebuild toàn bộ index của category theo batch (an toàn khi dữ liệu rất lớn) */
    public void rebuildCategoryIndex(int catId) {
        String key = RedisKeys.idxCatCreated(catId);
        // xoá khóa cũ (tuỳ chọn): nếu muốn incremental thì bỏ xóa, ở đây rebuild toàn phần
        redis.delete(key);

        int page = 0;
        int batch = 2000; // batch size — cân theo DB/heap
        while (true) {
            var ids = repo.findActiveIdsByCategoryOrderCreated(catId, PageRequest.of(page, batch));
            if (ids.isEmpty()) break;
            // set score = vị trí theo createdTime lấy từ DB? -> ta cần entity để score chính xác
            // Giản lược: nạp lại entity theo ids và tính score (đúng created_time)
            List<Product> chunk = repo.findAllById(ids);
            Set<ZSetOperations.TypedTuple<String>> tuples = chunk.stream()
                    .map(p -> new ZSetOperations
                            .TypedTuple<String>() {
                        final String value = String.valueOf(p.getId());
                        final Double score = ScoreUtil.scoreByCreated(p);
                        @Override public String getValue() { return value; }
                        @Override public Double getScore() { return score; }
                        @Override public int compareTo(org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> o) { return 0; }
                    })
                    .collect(Collectors.toSet());
            if (!tuples.isEmpty()) {
                redis.opsForZSet().add(key, tuples);
            }
            page++;
        }
    }
}
