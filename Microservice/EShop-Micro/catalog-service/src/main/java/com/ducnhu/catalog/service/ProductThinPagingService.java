package com.ducnhu.catalog.service;


import com.ducnhu.catalog.dto.ProductDTO;
import com.ducnhu.catalog.elastic.ProductSearchDoc;
import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.helper.FilterHash;
import com.ducnhu.catalog.helper.PageIndex;
import com.ducnhu.catalog.helper.ProductFilter;
import com.ducnhu.catalog.mapper.ProductMapper;
import com.ducnhu.catalog.repository.ProductRepository;
import com.ducnhu.catalog.repository.ProductSearchRepository;
import com.ducnhu.common.cache.CacheKey;
import com.ducnhu.common.cache.CacheTtl;
import com.ducnhu.common.cache.RedisCacheService;
import com.ducnhu.common.dto.PageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductThinPagingService {

    private final ProductRepository productRepo;
    private final RedisCacheService cache;
    private final StringRedisTemplate redis;
    private final ProductSearchRepository productSearchRepo;

    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_SIZE = 10;

    @Resource(name = "ioTaskExecutor")
    private Executor io;
    @Resource(name = "cpuTaskExecutor")
    private Executor cpu;

    public PageResponse<ProductDTO> list(ProductFilter filter, int page) {

        int size = (filter.size() == null || filter.size() <= 0) ? DEFAULT_SIZE : filter.size();
        if (size > MAX_SIZE) size = MAX_SIZE;

        // 1) Lấy version token theo category
        String catVer = (filter.categoryId() != null) ? redis.opsForValue().get(CacheKey.catVer(filter.categoryId())) : null;

        // 2) Tính filterHash
        String filterHash = FilterHash.of(filter, catVer);

        // 3) Tạo key trang mỏng
        String key = CacheKey.prodList(filterHash, page);

        // 4) Chọn TTL: search → ngắn; category → dài
        boolean isSearch = (filter.keyword() != null && !filter.keyword().isBlank());
        boolean isGlobalHot = (filter.categoryId() == null && filter.keyword() == null);
        Duration ttl;
        if (isSearch) {
            ttl = CacheTtl.PAGE_INDEX_SEARCH; // Ngắn
        } else if (isGlobalHot) {
            ttl = Duration.ofMinutes(30); // Trang chủ nên cache lâu hơn chút, chấp nhận delay
        } else {
            ttl = CacheTtl.PAGE_INDEX;    // Cache danh mục bình thường
        }

        int finalSize = size; // biến effective final cho lambda
        // 5) Lấy PageIndex từ cache hoặc DB
        PageIndex pi = cache.getOrLoad(key, new TypeReference<PageIndex>(){}, ttl,
                () -> loadPageIndexFromDb(filter, page, finalSize)
        );

        // 6) Hydrate DTO theo IDs (ưu tiên lấy ProductDTO từ cache prod:id:{id})
        List<ProductDTO> items = hydrateProductDtos(pi.ids);

        // 7) Prefetch p±1 (chỉ PageIndex, không hydrate để nhẹ)
        prefetchNearbyAsync(filter, page, filterHash, ttl);

        // 8) Trả PageResponse DTO
        return new PageResponse<>(page, finalSize, pi.total, pi.totalPages, items);
    }

    private PageIndex loadPageIndexFromDb(ProductFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, toSort(filter));

        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            Page<ProductSearchDoc> esPage = productSearchRepo.searchByKeyword(filter.keyword(), pageable);
            // --- THÊM LOG DEBUG TẠI ĐÂY ---
            System.out.println(">>> DEBUG ES SEARCH: Keyword = " + filter.keyword());
            System.out.println(">>> Found in ES: " + esPage.getTotalElements() + " items");
            esPage.getContent().forEach(doc -> System.out.println("   -> Doc ID: " + doc.getId()));
            // ------------------------------
            List<Integer> ids = esPage.getContent().stream()
                    .map(ProductSearchDoc::getId)
                    .toList();

            // 3. Đóng gói vào PageIndex để Cache Redis lưu lại
            return new PageIndex(ids, page, size, esPage.getTotalElements(), esPage.getTotalPages());
        }

        Page<Integer> idPage = productRepo.findIdsByCategory(
                filter.categoryId(),
                filter.minReviewCount(),
                pageable
        );
        return new PageIndex(idPage.getContent(), page, size, idPage.getTotalElements(), idPage.getTotalPages());    }

    private Sort toSort(ProductFilter filter) {
        if (filter.sort() == null || filter.sort().isBlank()) return Sort.unsorted();
        return "desc".equalsIgnoreCase(filter.dir())
                ? Sort.by(filter.sort()).descending()
                : Sort.by(filter.sort()).ascending();
    }

    private List<ProductDTO> hydrateProductDtos(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        // 1) multiGet raw JSON cho từng item
        List<String> keys = ids.stream().map(CacheKey::prodById).toList();
        List<String> raws = redis.opsForValue().multiGet(keys);

        List<ProductDTO> out = new ArrayList<>(ids.size());
        List<Integer> miss = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            String raw = (raws != null && i < raws.size()) ? raws.get(i) : null;
            if (raw == null || "__NULL__".equals(raw)) {
                miss.add(ids.get(i));
                continue;
            }
            try {
                ProductDTO dto = cache.getObjectMapper().readValue(raw, ProductDTO.class);
                out.add(dto);
            } catch (Exception e) {
                miss.add(ids.get(i)); // JSON bẩn -> nạp lại
            }
        }

        // 2) batch DB cho phần miss -> map DTO -> ghi cache prod:id:{id}
        if (!miss.isEmpty()) {
            List<Product> db = productRepo.findAllByIdIn(miss);
            Map<Integer, Product> map = db.stream().collect(Collectors.toMap(Product::getId, x -> x));
            for (Integer id : miss) {
                Product p = map.get(id);
                ProductDTO dto = (p == null ? null : ProductMapper.toDto(p));
                out.add(dto);
                cache.put(CacheKey.prodById(id), dto, CacheTtl.ITEM_DETAIL); // LƯU DTO, KHÔNG LƯU ENTITY
            }
        }

        // 3) Giữ đúng thứ tự theo ids
        out.sort(Comparator.comparingInt(o -> ids.indexOf(((ProductDTO) o).id())));
        return out;
    }

    /**
     * Prefetch p+1 (và p-1) chỉ PageIndex để tăng hit-rate
     */
    private void prefetchNearbyAsync(ProductFilter f, int page, String filterHash, Duration ttl) {
        io.execute(() -> {
            int next = page + 1;
            String keyNext = CacheKey.prodList(filterHash, next);
            if (!cache.hasKey(keyNext)) {
                PageIndex nextPi = loadPageIndexFromDb(f, next, DEFAULT_SIZE);
                cache.put(keyNext, nextPi, ttl);
            }
            int prev = page - 1;
            if (prev >= 1) {
                String keyPrev = CacheKey.prodList(filterHash, prev);
                if (!cache.hasKey(keyPrev)) {
                    PageIndex prevPi = loadPageIndexFromDb(f, prev, DEFAULT_SIZE);
                    cache.put(keyPrev, prevPi, ttl);
                }
            }
        });
    }

    /**
     * Gọi khi admin sửa category/di chuyển product → invalidate rẻ bằng INCR token
     */
    public void bumpCategoryVersion(Integer catId) {
        redis.opsForValue().increment(CacheKey.catVer(catId));
    }
}