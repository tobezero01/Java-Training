package com.ducnhu.catalog.scheduling.worker;

import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.repository.ProductRepository;
import com.ducnhu.catalog.service.ProductIndexService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductIndexRefreshJob {
    private final ProductRepository productRepository;
    private final ProductIndexService index;
    private final StringRedisTemplate redisTemplate;

    @Scheduled(cron = "${jobs.aliasIndex.cron}")
    @SchedulerLock(name = "productIndexRefreshJob", lockAtMostFor = "PT5M")
    public void refresh() {
        Pageable pageable = PageRequest.of(0, 150, Sort.by("updatedTime").descending());
        Page<Product> page = productRepository.findAll(pageable);

        for (Product product : page.getContent()) {
            index.markIdExists(product.getId());
            if (product.getAlias() != null && !product.getAlias().isBlank()) {
                index.setAliasToId(product.getAlias(), product.getId());
            }
        }
    }
}
