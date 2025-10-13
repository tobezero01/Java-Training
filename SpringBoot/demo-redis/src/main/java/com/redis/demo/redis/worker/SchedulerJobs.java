package com.redis.demo.redis.worker;

import com.redis.demo.redis.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchedulerJobs {
    private static final Logger log = LoggerFactory.getLogger(SchedulerJobs.class);

    private final ProductService productService;
    private final StringRedisTemplate redis;

    public SchedulerJobs(ProductService productService, StringRedisTemplate redis) {
        this.productService = productService;
        this.redis = redis;
    }

    // Mỗi 30 phút đẩy lại (00, 30) — tùy chỉnh theo nhu cầu
    @Scheduled(cron = "0 0/30 * * * *")
    public void enqueueAllProducts() {
        List<Integer> ids = productService.getAllActiveIds();
        if (ids.isEmpty()) return;
        for (Integer id : ids) {
            redis.opsForList().leftPush("queue:product", String.valueOf(id));
        }
        log.info("Enqueued {} product ids for cache warmup", ids.size());
    }
}
