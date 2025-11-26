package com.ducnhu.catalog.elastic;

import com.ducnhu.catalog.repository.ProductSearchRepository;
import com.ducnhu.catalog.service.ProductIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductAliasResolver {
    private final ProductSearchRepository esRepo;
    private final ProductIndexService redisIndex;

    public Integer resolveId(String alias) {
        // 1) ưu tiên Elasticsearch
        Optional<ProductSearchDoc> doc = esRepo.findByAlias(alias);
        if (doc.isPresent()) return doc.get().getId();

        // 2) fallback Redis mapping (nếu vẫn muốn giữ trong giai đoạn quá độ)
        Integer id = redisIndex.resolveAlias(alias);
        return id; // có thể null -> controller/service sẽ quăng 404 như cũ
    }
}
