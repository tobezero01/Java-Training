package com.redis.demo.redis.service;

import com.redis.demo.redis.config.CacheTtl;
import com.redis.demo.redis.mapper.MapperUtil;
import com.redis.demo.redis.mapper.ProductDto;
import com.redis.demo.redis.repo.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final RedisCacheService cache;

    public ProductService(ProductRepository productRepository, RedisCacheService cache) {
        this.productRepository = productRepository;
        this.cache = cache;
    }

    private String productKey(int id) { return "product:" + id; }
    private String relatedKey(int id) { return "related_product:" + id; }

    public ProductDto getProductById(int id) {
        return cache.getOrLoad(
                productKey(id),
                ProductDto.class,
                CacheTtl.MEDIUM,
                () -> productRepository.findActiveProductWithMedia(id)
                        .map(MapperUtil::toDto)
                        .orElse(null)
        );
    }

    /** Tính danh sách ID sản phẩm liên quan và cache lại */
    public List<Integer> getRelatedProductIds(int id) {
        // Lấy categoryId trước (từ DTO có sẵn)
        ProductDto dto = getProductById(id);
        if (dto == null || dto.categoryId == null) {
            return Collections.emptyList();
        }

        int catId = dto.categoryId;
        return cache.getOrLoad(
                relatedKey(id),
                new TypeReference<List<Integer>>() {},
                CacheTtl.MEDIUM,
                () -> productRepository.findRelatedIds(catId, id, PageRequest.of(0, 5))
        );
    }

    /** Dùng khi worker muốn build cache trực tiếp, bỏ qua getOrLoad */
    public void rebuildCachesForProduct(int id) {
        productRepository.findActiveProductWithMedia(id).ifPresentOrElse(p -> {
            // Put chi tiết
            ProductDto dto = MapperUtil.toDto(p);
            cache.put(productKey(id), dto, CacheTtl.MEDIUM);
            // Put related
            List<Integer> related = productRepository.findRelatedIds(
                    p.getCategory() != null ? p.getCategory().getId() : -1, id, PageRequest.of(0, 5));
            cache.put(relatedKey(id), related, CacheTtl.MEDIUM);
        }, () -> {
            // Không có dữ liệu -> đánh dấu NULL
            cache.put(productKey(id), null, CacheTtl.MEDIUM);
            cache.put(relatedKey(id), Collections.emptyList(), CacheTtl.MEDIUM);
        });
    }

    public List<Integer> getAllActiveIds() {
        return productRepository.findAllActiveIds();
    }
}
