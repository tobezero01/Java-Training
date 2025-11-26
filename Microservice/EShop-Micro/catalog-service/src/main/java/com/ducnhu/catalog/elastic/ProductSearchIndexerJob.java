package com.ducnhu.catalog.elastic;

import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.repository.ProductRepository;
import com.ducnhu.catalog.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductSearchIndexerJob {
    private final ProductRepository productRepo;
    private final ProductSearchRepository esRepo;

    // chạy mỗi giờ (điều chỉnh theo nhu cầu)
    @Scheduled(cron = "0 0 * * * *")
    public void refreshAliasIndex() {
        Pageable pageable = PageRequest.of(0, 500, Sort.by("createdTime").descending());
        Page<Product> page = productRepo.findAll(pageable);

        List<ProductSearchDoc> docs = new ArrayList<>(page.getNumberOfElements());
        for (Product p : page.getContent()) {
            ProductSearchDoc d = new ProductSearchDoc(
                    p.getId(),
                    p.getAlias(),
                    p.getAlias(), // aliasNgram dùng luôn alias
                    p.getName(),
                    (p.getCategory() == null ? null : p.getCategory().getName())
            );
            docs.add(d);
        }
        esRepo.saveAll(docs);
    }
}
