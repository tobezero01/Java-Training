package com.redis.demo.redis.worker;

import com.redis.demo.redis.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueWorker {
    private static final Logger log = LoggerFactory.getLogger(QueueWorker.class);

    private final StringRedisTemplate redis;
    private final ProductService productService;

    public QueueWorker(StringRedisTemplate redis, ProductService productService) {
        this.redis = redis;
        this.productService = productService;
    }

    // Mỗi 1s thì thử lấy tối đa 20 item (tránh loop busy)
    @Scheduled(fixedDelay = 1000)
    public void drainQueue() {
        int batch = 0;
        for (; batch < 20; batch++) {
            String idStr = redis.opsForList().rightPop("queue:product"); // POP phía right
            if (idStr == null) break; // hết job
            try {
                int id = Integer.parseInt(idStr);
                productService.rebuildCachesForProduct(id);
            } catch (Exception ex) {
                log.warn("Failed to rebuild cache for id={}, err={}", idStr, ex.getMessage());
            }
        }
        if (batch > 0) {
            log.info("QueueWorker processed {} items", batch);
        }
    }
}
