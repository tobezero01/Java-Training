package com.redis.demo.redis.worker;

import com.redis.demo.redis.repo.ProductRepository;
import com.redis.demo.redis.service.ProductService;
import com.redis.demo.redis.service.RedisIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CronJob:
 * - 1) Rebuild index cho các category (lịch thưa) — an toàn khi dữ liệu lớn (batch)
 * - 2) Warm detail cache cho một lượng nhỏ sản phẩm mới nhất (lịch dày hơn)
 *
 * Lưu ý: ở prod bạn nên thay rebuild toàn phần bằng incremental theo watermark updated_time.
 */
@Component
public class IndexMaintenanceJobs {
    private static final Logger log = LoggerFactory.getLogger(IndexMaintenanceJobs.class);

    private final RedisIndexService index;
    private final ProductRepository repo;
    private final ProductService productService;

    public IndexMaintenanceJobs(RedisIndexService index, ProductRepository repo, ProductService productService) {
        this.index = index;
        this.repo = repo;
        this.productService = productService;
    }

    /** (Ví dụ) 3 giờ chạy 1 lần: rebuild ZSET cho mọi category (batch trong service) */
//    @Scheduled(cron = "0 0 */3 * * *")
    @Scheduled(cron = "0 */1 * * * *")
    public void rebuildAllCategoriesIndex() {
        List<Integer> catIds = repo.findAllActiveCategoryIds();
        int ok = 0;
        for (Integer catId : catIds) {
            try {
                index.rebuildCategoryIndex(catId);
                ok++;
            } catch (Exception e) {
                log.warn("Rebuild index failed for catId={}, err={}", catId, e.getMessage());
            }
        }
        log.info("Rebuild index done: {} / {} categories", ok, catIds.size());
    }

    /** (Ví dụ) Mỗi 5 phút: warm chi tiết cho 100 sp mới nhất của mỗi category */
    @Scheduled(cron = "0 */5 * * * *")
    public void warmNewestDetails() {
        List<Integer> catIds = repo.findAllActiveCategoryIds();
        for (Integer catId : catIds) {
            List<Integer> ids = index.getPageIdsByCategory(catId, /*page*/1, /*size*/100);
            for (Integer id : ids) {
                try {
                    productService.getProductById(id); // getOrLoad -> fill cache nếu miss
                } catch (Exception ignored) {}
            }
        }
        log.info("Warm newest details done for {} categories", catIds.size());
    }
}
