package com.ducnhu.catalog.scheduling.worker;


import com.ducnhu.catalog.dto.ProductDTO;
import com.ducnhu.catalog.entity.Category;
import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.mapper.ProductMapper;
import com.ducnhu.catalog.repository.CategoryRepository;
import com.ducnhu.catalog.repository.ProductRepository;
import com.ducnhu.catalog.scheduling.SortScenario;
import com.ducnhu.common.cache.CacheKey;
import com.ducnhu.common.cache.CacheTtl;
import com.ducnhu.common.cache.RedisCacheService;
import com.ducnhu.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductPageWarmupJob {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository; // Inject thêm để lấy danh mục động
    private final RedisCacheService cacheService;
    private final StringRedisTemplate redisTemplate;

    // Cấu hình Warmup
    private static final int PAGE_SIZE = 20;
    private static final int MAX_PAGES_TO_WARM = 3;

    @Scheduled(cron = "${jobs.pageWarmup.cron}")
    @SchedulerLock(name = "productPageWarmupJob", lockAtMostFor = "PT20M")
    @Transactional(readOnly = true)
    public void warm() {
        log.info("START: Warming up Cache for Top Rated & Most Reviewed products...");

        List<SortScenario> scenarios = List.of(

                // Kịch bản A: Nhiều nhận xét nhất
                // Frontend gửi: ?sort=reviewCount&dir=desc
                new SortScenario("reviewCount", "desc",
                        Sort.by("reviewCount").descending()),

                // Kịch bản B: Điểm đánh giá cao nhất
                // Frontend gửi: ?sort=averageRating&dir=desc
                // Logic nâng cao: Rating cao NHƯNG phải ưu tiên nhiều review (tránh 5 sao ảo 1 review)
                new SortScenario("averageRating", "desc",
                        Sort.by("averageRating").descending().and(Sort.by("reviewCount").descending()))
        );
        List<Integer> targetCategoryIds = new ArrayList<>();
        targetCategoryIds.add(null);

        List<Integer> topLevelIds = categoryRepository.findAllTopLevel().stream()
                .map(Category::getId).toList();
        targetCategoryIds.addAll(topLevelIds);

        for (Integer catId : targetCategoryIds) {
            String catVer = (catId != null)
                    ? redisTemplate.opsForValue().get(CacheKey.catVer(catId))
                    : ""; // Nếu null catId (trang chủ) thì ko có version hoặc handle riêng

            for (SortScenario scenario : scenarios) {
                warmUpScenario(catId, catVer, scenario);
            }
        }
        log.info("END: Finished warming up.");
    }

    private void warmUpScenario(Integer catId, String catVer, SortScenario scenario) {
        for (int page = 1; page <= MAX_PAGES_TO_WARM; page ++) {
            String key = CacheKey.ns("prod:byCat:v2",
                    "cat", String.valueOf(catId),
                    "p", String.valueOf(page),
                    "s", scenario.fieldName(), // "reviewCount" hoặc "averageRating"
                    "d", scenario.direction(), // "desc"
                    "ver", catVer == null ? "" : catVer
            );
            if (cacheService.hasKey(key)) continue;

            try {
                Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE, scenario.sortLogic());
                Page<Product> p;
                if (catId == null) {
                    p = productRepository != null ? productRepository.findAll(pageable) : null;
                } else {
                    p = productRepository.findPageByCategory(catId, pageable);
                }

                List<ProductDTO> items = p.getContent().stream()
                        .map(ProductMapper::toDto).toList();
                PageResponse<ProductDTO> pageResponse = new PageResponse<>(
                        page, p.getSize(), p.getTotalElements(), p.getTotalPages(), items
                );
                // Lưu Cache (TTL dài - ví dụ 1 giờ - vì review/rating không thay đổi từng giây)
                cacheService.put(key, pageResponse, CacheTtl.PRODUCT);
            } catch (Exception e) {
                log.error("Error warming catId={} sort={} page={}", catId, scenario.fieldName(), page, e);
            }
        }
    }
}
