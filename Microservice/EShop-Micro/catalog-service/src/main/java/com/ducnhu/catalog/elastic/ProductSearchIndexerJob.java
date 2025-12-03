package com.ducnhu.catalog.elastic;

import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.repository.ProductRepository;
import com.ducnhu.catalog.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSearchIndexerJob {

    private final ProductRepository productRepo;
    private final ProductSearchRepository esRepo;
    private final StringRedisTemplate redisTemplate;

    // Key lưu con trỏ trong Redis
    private static final String REDIS_CURSOR_KEY = "job:product:index:cursor_id";
    private static final int BATCH_SIZE = 200; // Mỗi lần chạy xử lý 1000 sp

    // "0 */2 * * * *" = Mỗi 2 phút
    @Scheduled(cron = "0 */2 * * * *")
    public void indexBatch() {
        // 1. Lấy ID cuối cùng đã xử lý từ Redis
        String cursorStr = redisTemplate.opsForValue().get(REDIS_CURSOR_KEY);
        int lastId = (cursorStr == null) ? 0 : Integer.parseInt(cursorStr);

        log.info("JOB START: Indexing batch starting from ID > {}", lastId);

        // Lưu ý: Bạn cần thêm hàm findByIdGreaterThan vào ProductRepository nếu chưa có (xem bước 3)
        Pageable limit = PageRequest.of(0, BATCH_SIZE);
        List<Product> products = productRepo.findByIdGreaterThan(lastId, limit);

        // 3. Kiểm tra kết quả
        if (products.isEmpty()) {
            log.info("JOB FINISHED: No more products to index. (Last ID: {})", lastId);
            // Tuỳ chọn: Reset về 0 để vòng sau chạy lại từ đầu (Rolling Update)
            // redisTemplate.delete(REDIS_CURSOR_KEY);
            return;
        }

        // 4. Convert & Index vào Elasticsearch
        List<ProductSearchDoc> docs = new ArrayList<>(products.size());
        for (Product p : products) {
            // Logic map dữ liệu
            ProductSearchDoc d = new ProductSearchDoc(
                    p.getId(),
                    p.getAlias(),
                    p.getAlias(),
                    p.getName(),
                    (p.getCategory() == null ? null : p.getCategory().getName())
            );
            docs.add(d);
        }
        esRepo.saveAll(docs);

        // 5. Cập nhật con trỏ mới vào Redis
        Integer maxIdInBatch = products.get(products.size() - 1).getId();
        redisTemplate.opsForValue().set(REDIS_CURSOR_KEY, String.valueOf(maxIdInBatch));

        log.info("JOB SUCCESS: Indexed {} items. New Cursor ID: {}", products.size(), maxIdInBatch);
    }

    // Hàm tiện ích để Admin reset chạy lại từ đầu (Gọi qua Controller)
    public void resetCursor() {
        redisTemplate.delete(REDIS_CURSOR_KEY);
        log.info("JOB RESET: Cursor deleted. Next run will start from 0.");
    }
}