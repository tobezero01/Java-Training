package com.redis.demo.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.redis.demo.redis.config.CacheTtl;
import com.redis.demo.redis.entity.Product;
import com.redis.demo.redis.mapper.MapperUtil;
import com.redis.demo.redis.mapper.PageResult;
import com.redis.demo.redis.mapper.ProductDto;
import com.redis.demo.redis.mapper.ProductSummary;
import com.redis.demo.redis.repo.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dịch vụ cấp trang theo category nhờ Redis index IDs.
 * - getCategoryPage: từ ZSET -> IDs -> batch-get chi tiết
 * - getByIds: client gửi IDs -> trả chi tiết giữ thứ tự
 */
@Service
public class ProductPageService {

    private final RedisIndexService index;
    private final ProductRepository repo;
    private final RedisCacheService cache;

    public ProductPageService(RedisIndexService index, ProductRepository repo, RedisCacheService cache) {
        this.index = index;
        this.repo = repo;
        this.cache = cache;
    }

    /** Lấy trang sản phẩm theo category: page IDs -> chi tiết */
    public PageResult<ProductSummary> getCategoryPage(int catId, int page, int size) {
        // 1) Lấy danh sách ID từ Redis
        List<Integer> ids = index.getPageIdsByCategory(catId, page, size);
        if (ids.isEmpty()) return new PageResult<>(List.of(), false);

        // 2) Batch get chi tiết tối thiểu (summary) từ cache -> DB
        List<ProductSummary> items = getSummariesByIds(ids);

        // 3) hasNext = true nếu số item == size (đơn giản)
        boolean hasNext = (items.size() == size);
        return new PageResult<>(items, hasNext);
    }

    /** Client đưa list IDs lên và xin chi tiết product (full DTO) */
    public List<ProductDto> getByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        // Thử đọc cache -> nếu miss -> nạp DB -> put cache
        // Dùng danh sách để giữ đúng thứ tự đầu vào
        Map<Integer, ProductDto> map = new LinkedHashMap<>();

        for (Integer id : ids) {
            ProductDto cached = cache.getOrLoad(
                    "product:" + id,
                    ProductDto.class,
                    CacheTtl.MEDIUM,
                    () -> repo.findActiveProductWithMedia(id)
                            .map(MapperUtil::toDto)
                            .orElse(null)
            );
            if (cached != null) map.put(id, cached);
        }
        // trả theo thứ tự ids
        return ids.stream().map(map::get).filter(Objects::nonNull).toList();
    }

    /** Lấy ProductSummary nhanh gọn cho listing (ít trường) */
    private List<ProductSummary> getSummariesByIds(List<Integer> ids) {
        // Cố gắng tận dụng cache chi tiết trước (nếu có) để map sang summary
        Map<Integer, ProductDto> cachedMap = new HashMap<>();
        for (Integer id : ids) {
            var opt = cache.getIfPresent("product:" + id, ProductDto.class);
            opt.ifPresent(dto -> cachedMap.put(id, dto));
        }

        // Với ID miss cache -> hit DB một lần
        List<Integer> miss = ids.stream().filter(i -> !cachedMap.containsKey(i)).toList();
        if (!miss.isEmpty()) {
            List<Product> db = repo.findActiveByIds(miss);
            // put cache cho từng item
            for (Product p : db) {
                ProductDto dto = MapperUtil.toDto(p);
                cache.put("product:" + p.getId(), dto, CacheTtl.MEDIUM);
                cachedMap.put(p.getId(), dto);
            }
        }

        // Trả về summary theo đúng thứ tự ban đầu
        return ids.stream()
                .map(cachedMap::get)
                .filter(Objects::nonNull)
                .map(dto -> new ProductSummary(dto.id, dto.name, dto.price, dto.mainImage))
                .collect(Collectors.toList());
    }
}