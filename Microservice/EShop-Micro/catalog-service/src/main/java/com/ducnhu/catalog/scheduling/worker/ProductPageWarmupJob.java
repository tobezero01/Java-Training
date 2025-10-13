package com.ducnhu.catalog.scheduling.worker;


import com.ducnhu.catalog.dto.ProductDTO;
import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.mapper.ProductMapper;
import com.ducnhu.catalog.repository.ProductRepository;
import com.ducnhu.common.cache.CacheKey;
import com.ducnhu.common.cache.CacheTtl;
import com.ducnhu.common.cache.RedisCacheService;
import com.ducnhu.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductPageWarmupJob {
    private final ProductRepository productRepository;
    private final RedisCacheService cacheService;
    private final StringRedisTemplate redisTemplate;

    private static final List<Integer> HOT_CATS = List.of(2,4,6);
    private static final int PAGE_SIZE = 20;
    private static final int MAX_PAGES_PER_CAT = 3;

    @Scheduled(cron = "${jobs.pageWarmup.cron}")
    @SchedulerLock(name = "productPageWarmupJob", lockAtMostFor = "PT10M")
    public void warm() {
        for (Integer catId : HOT_CATS) {
            String ver = redisTemplate.opsForValue().get(CacheKey.catVer(catId));
            for (int page = 1; page <= MAX_PAGES_PER_CAT; page++) {
                String key = CacheKey.ns("prod:byCat:v2",
                        "cat", String.valueOf(catId),
                        "p", String.valueOf(page),
                        "s", "", "d", "",
                        "ver", ver == null ? "" : ver);

                if (cacheService.hasKey(key)) continue;

                Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE);
                Page<Product> p = productRepository.findPageByCategory(catId, pageable);
                List<ProductDTO> items = p.getContent().stream()
                        .map(ProductMapper::toDto).toList();

                PageResponse<ProductDTO> pageDto = new PageResponse<>(
                        page, p.getSize(), p.getTotalElements(), p.getTotalPages(), items
                );

                cacheService.put(key, pageDto, CacheTtl.PRODUCT);
            }
        }
    }
}
