package com.ducnhu.catalog.scheduling.worker;


import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReviewStatsBackfillJob {

    private final ProductRepository productRepo;

    @Scheduled(cron = "${jobs.ratingBackfill.cron}")
    @SchedulerLock(name = "reviewStatsBackfillJob")
    @Transactional
    public void run() {
        Pageable page = PageRequest.of(0, 200, Sort.by("id").descending());
        Page<Product> productPage = productRepo.findAll(page);
        for (Product prod : productPage) {
            productRepo.updateReviewCountAndAverageRating(prod.getId());
        }
    }
}
