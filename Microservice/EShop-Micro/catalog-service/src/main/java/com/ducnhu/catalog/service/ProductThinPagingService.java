package com.ducnhu.catalog.service;


import com.ducnhu.catalog.dto.ProductDTO;
import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.helper.FilterHash;
import com.ducnhu.catalog.helper.PageIndex;
import com.ducnhu.catalog.helper.ProductFilter;
import com.ducnhu.catalog.mapper.ProductMapper;
import com.ducnhu.catalog.repository.ProductRepository;
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

/**
 * Thin cache:
 * - Cache mỏng: prod:list:{filterHash}:p:{page} -> chỉ IDs + meta (PageIndex)
 * - Cache chi tiết: prod:id:{id} -> JSON ProductDTO (không cache entity)
 * - Prefetch trang lân cận (p+1, p-1) chỉ PageIndex (không hydrate)
 * - Invalidation rẻ bằng INCR cat:ver:{catId}
 */
@Service
@RequiredArgsConstructor
public class ProductThinPagingService {

    private final ProductRepository productRepo;
    private final RedisCacheService cache;
    private final StringRedisTemplate redis;

    @Resource(name="ioTaskExecutor")  private Executor io;
    @Resource(name="cpuTaskExecutor") private Executor cpu;

    private static final int PAGE_SIZE = 20;

    /** API chính: trả PageResponse<DTO> để controller không cần map nữa */
    public PageResponse<ProductDTO> list(ProductFilter filter, int page) {
        // 1) Lấy version token theo category
        String catVer = (filter.categoryId()!=null) ? redis.opsForValue().get(CacheKey.catVer(filter.categoryId())) : null;

        // 2) Tính filterHash
        String filterHash = FilterHash.of(filter, catVer);

        // 3) Tạo key trang mỏng
        String key = CacheKey.prodList(filterHash, page);

        // 4) Chọn TTL: search → ngắn; category → dài
        boolean isSearch = (filter.keyword()!=null && !filter.keyword().isBlank());
        Duration ttl = isSearch ? CacheTtl.PAGE_INDEX_SEARCH : CacheTtl.PAGE_INDEX;

        // 5) Lấy PageIndex từ cache hoặc DB
        PageIndex pi = cache.getOrLoad(
                key,
                new TypeReference<PageIndex>(){},
                ttl,
                () -> loadPageIndexFromDb(filter, page)
        );

        // 6) Hydrate DTO theo IDs (ưu tiên lấy ProductDTO từ cache prod:id:{id})
        List<ProductDTO> items = hydrateProductDtos(pi.ids);

        // 7) Prefetch p±1 (chỉ PageIndex, không hydrate để nhẹ)
        prefetchNearbyAsync(filter, page, filterHash, ttl);

        // 8) Trả PageResponse DTO
        return new PageResponse<>(
                page,
                PAGE_SIZE,
                pi.total,
                pi.totalPages,
                items
        );
    }

    /** Query DB để build PageIndex (chỉ IDs) */
    private PageIndex loadPageIndexFromDb(ProductFilter filter, int page) {
        Pageable pageable = PageRequest.of(page-1, PAGE_SIZE, toSort(filter));
        Page<Integer> idPage = (filter.keyword()!=null && !filter.keyword().isBlank())
                ? productRepo.findIdsByKeyword(filter.keyword(), pageable)
                : productRepo.findIdsByCategory(filter.categoryId(), pageable);

        return new PageIndex(idPage.getContent(), page, PAGE_SIZE, idPage.getTotalElements(), idPage.getTotalPages());
    }

    private Sort toSort(ProductFilter filter) {
        if (filter.sort()==null || filter.sort().isBlank()) return Sort.unsorted();
        return "desc".equalsIgnoreCase(filter.dir())
                ? Sort.by(filter.sort()).descending()
                : Sort.by(filter.sort()).ascending();
    }

    /** Hydrate: đọc ProductDTO từ Redis; miss -> DB batch (Product) -> map DTO -> ghi lại cache item */
    private List<ProductDTO> hydrateProductDtos(List<Integer> ids) {
        if (ids==null || ids.isEmpty()) return Collections.emptyList();

        // 1) multiGet raw JSON cho từng item
        List<String> keys = ids.stream().map(CacheKey::prodById).toList();
        List<String> raws = redis.opsForValue().multiGet(keys);

        List<ProductDTO> out = new ArrayList<>(ids.size());
        List<Integer> miss = new ArrayList<>();

        for (int i=0;i<ids.size();i++){
            String raw = (raws!=null && i<raws.size()) ? raws.get(i) : null;
            if (raw==null || "__NULL__".equals(raw)) { miss.add(ids.get(i)); continue; }
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
            Map<Integer,Product> map = db.stream().collect(Collectors.toMap(Product::getId, x->x));
            for (Integer id : miss) {
                Product p = map.get(id);
                ProductDTO dto = (p==null ? null : ProductMapper.toDto(p));
                out.add(dto);
                cache.put(CacheKey.prodById(id), dto, CacheTtl.ITEM_DETAIL); // LƯU DTO, KHÔNG LƯU ENTITY
            }
        }

        // 3) Giữ đúng thứ tự theo ids
        out.sort(Comparator.comparingInt(o -> ids.indexOf(((ProductDTO)o).id())));
        return out;
    }

    /** Prefetch p+1 (và p-1) chỉ PageIndex để tăng hit-rate */
    private void prefetchNearbyAsync(ProductFilter f, int page, String filterHash, Duration ttl) {
        io.execute(() -> {
            int next = page + 1;
            String keyNext = CacheKey.prodList(filterHash, next);
            if (!cache.hasKey(keyNext)) {
                PageIndex nextPi = loadPageIndexFromDb(f, next);
                cache.put(keyNext, nextPi, ttl);
            }
            int prev = page - 1;
            if (prev >= 1) {
                String keyPrev = CacheKey.prodList(filterHash, prev);
                if (!cache.hasKey(keyPrev)) {
                    PageIndex prevPi = loadPageIndexFromDb(f, prev);
                    cache.put(keyPrev, prevPi, ttl);
                }
            }
        });
    }

    /** Gọi khi admin sửa category/di chuyển product → invalidate rẻ bằng INCR token */
    public void bumpCategoryVersion(Integer catId) {
        redis.opsForValue().increment(CacheKey.catVer(catId));
    }
}