package com.ducnhu.catalog.service;

import com.ducnhu.catalog.dto.ProductDTO;
import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.helper.ProductFilter;
import com.ducnhu.catalog.mapper.ProductMapper;
import com.ducnhu.catalog.repository.ProductRepository;
import com.ducnhu.common.cache.CacheKey;
import com.ducnhu.common.cache.CacheTtl;
import com.ducnhu.common.cache.RedisCacheService;
import com.ducnhu.common.dto.PageResponse;
import com.ducnhu.common.exception.ProductNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    public static final int RESULT_PER_PAGE = 20;

    private final ProductRepository productRepository;
    private final RedisCacheService redisCacheService;
    private final ProductThinPagingService productThinPagingService;
    private final ProductIndexService productIndexService;
    private final StringRedisTemplate redisTemplate;
    /**
     * (Cảnh báo) Với category cực lớn, phương thức này vẫn tốn bộ nhớ; ưu tiên dùng listByCategoryPaged
     */
    @Override
    public PageResponse<ProductDTO> listByCategory(Integer categoryId, int page, String sort, String dir) {
        String catVer = (categoryId != null) ?  redisTemplate.opsForValue().get(CacheKey.catVer(categoryId)) : null;

        String key = CacheKey.ns(
                "prod:byCat:v2",
                "cat", String.valueOf(categoryId),
                "p",   String.valueOf(page),
                "s",   (sort == null ? "" : sort),
                "d",   (dir  == null ? "" : dir),
                "ver", (catVer == null ? "" : catVer)
        );

        return redisCacheService.getOrLoad(
                key,
                new TypeReference<PageResponse<ProductDTO>>() {},
                CacheTtl.PRODUCT,
                () -> {
                    Sort srt = Sort.unsorted();
                    if (sort != null && !sort.isBlank()) {
                        srt = "desc".equalsIgnoreCase(dir)
                                ? Sort.by(sort).descending()
                                : Sort.by(sort).ascending();
                    }
                    Pageable pageable = PageRequest.of(page - 1, RESULT_PER_PAGE, srt);

                    Page<Product> p = productRepository.findPageByCategory(categoryId, pageable);
                    List<ProductDTO> items = p.getContent()
                            .stream()
                            .map(ProductMapper::toDto)
                            .toList();

                    return new PageResponse<>(
                            page,
                            p.getSize(),
                            p.getTotalElements(),
                            p.getTotalPages(),
                            items
                    );
                }
        );
    }
    @Override
    public PageResponse<ProductDTO> listByCategoryPaged(Integer categoryId, int page, String sort, String dir) {
        ProductFilter filter = new ProductFilter(categoryId, null, sort, dir);
        return productThinPagingService.list(filter, page);
    }

    @Override
    public List<ProductDTO> listByCategoryNoPaging(Integer categoryId) {
        return productRepository.listByCategory(categoryId)
                .stream().map(ProductMapper::toDto).toList();
    }

    @Override
    public ProductDTO getProduct(String alias) throws ProductNotFoundException {
//        ProductDTO dto = redisCacheService.getOrLoad(
//                CacheKey.productByAlias(alias),
//                ProductDTO.class,
//                CacheTtl.PRODUCT,
//                () -> {
//                    Product product = productRepository.findByAlias(alias);
//                    return (product==null? null : ProductMapper.toDto(product));
//                }
//        );
//        if (dto == null) throw new ProductNotFoundException("Product not found with alias " + alias);
//        // LƯU Ý: CacheKey.productByAlias(alias) đang lưu DTO, KHÔNG lưu entity
//        return dto;
        // 1) Tra alias->id (nếu __NULL__ -> 404 nhanh)
        Integer id = productIndexService.resolveAlias(alias);
        if (id != null) {
            return getProduct(id); // tái sử dụng get-by-id (đã tối ưu bitmap + cache item)
        }

        // 2) Chưa có mapping -> đánh DB 1 lần
        Product p = productRepository.findByAlias(alias);
        if (p == null) {
            productIndexService.setAliasNull(alias); // cache âm TTL ngắn, chống spam
            throw new ProductNotFoundException("Product not found with alias " + alias);
        }

        // 3) Có bản ghi -> build DTO + set các chỉ mục
        ProductDTO dto = ProductMapper.toDto(p);

        productIndexService.setAliasToId(alias, p.getId());           // alias2id
        productIndexService.markIdExists(p.getId());                  // bitmap id
        redisCacheService.put(CacheKey.prodById(p.getId()), dto, CacheTtl.PRODUCT); // item DTO

        // 4) Trả về
        return dto;
    }

    @Override
    public ProductDTO getProduct(Integer id) throws ProductNotFoundException {
//        ProductDTO dto = redisCacheService.getOrLoad(
//                CacheKey.prodById(id),              // prod:id:{id} lưu DTO
//                ProductDTO.class,
//                CacheTtl.PRODUCT,
//                () -> productRepository.findById(id).map(ProductMapper::toDto).orElse(null)
//        );
//        if (dto == null) throw new ProductNotFoundException("Product not found with ID " + id);
//        return dto;

        // 1) Bitmap: nếu chắc chắn không tồn tại -> 404 sớm (O(1), không đập DB)
//        if (!productIndexService.idMayExist(id)) {
//            // (Lưu ý) Nếu DB vừa insert id mới mà bitmap chưa mark -> lần gọi đầu có thể 404 sớm.
//            // Tuỳ chọn: bỏ qua bước này trong môi trường cần "read-after-write mạnh".
//            throw new ProductNotFoundException("Product not found with ID " + id);
//        }

        // Đọc từ cache item DTO hoặc load DB
        ProductDTO dto = redisCacheService.getOrLoad(
                CacheKey.prodById(id),
                ProductDTO.class,
                CacheTtl.PRODUCT,
                () -> productRepository.findById(id).map(ProductMapper::toDto).orElse(null)
        );

        // Sau loader:
        if (dto == null) {
            // bitmap có thể stale (bit=1 nhưng DB xoá) -> tuỳ chọn clear bit
            // productIndexService.clearId(id);
            throw new ProductNotFoundException("Product not found with ID " + id);
        }

        // Đảm bảo bitmap được mark khi vừa load thành công từ DB
        productIndexService.markIdExists(id);
        return dto;
    }

    @Override
    public PageResponse<ProductDTO> search(String keyWord, int pageNum) {
        ProductFilter filter = new ProductFilter(null, keyWord, null, null);
        return productThinPagingService.list(filter, pageNum); // trả thẳng DTO
    }

//    @Override
//    @Transactional(readOnly = true)
//    public PageResponse<ProductDTO> getPurchasedProducts(Integer customerId, int page, String sort, String dir) {
//        boolean hasAny = productRepository.existsAnyCompletedPurchaseFromTrack(customerId, COMPLETED_STATUSES );
//        if (!hasAny) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Khách chưa hoàn thành đơn hàng nào.");
//
//        Sort srt = Sort.unsorted();
//        if (sort != null && !sort.isBlank()) {
//            srt = "desc".equalsIgnoreCase(dir) ? Sort.by(sort).descending() : Sort.by(sort).ascending();
//        }
//        Pageable pageable = PageRequest.of(page - 1, RESULT_PER_PAGE, srt);
//
//        Page<Product> p = productRepository.findPurchasedByCustomerAndStatuses(customerId, COMPLETED_STATUSES, pageable);
//        List<ProductDTO> items = p.getContent().stream().map(ProductMapper::toDto).toList();
//
//        return new PageResponse<>(
//                page,
//                p.getSize(),
//                p.getTotalElements(),
//                p.getTotalPages(),
//                items
//        );
//    }

//    private static final List<OrderStatus> COMPLETED_STATUSES =
//            List.of(OrderStatus.DELIVERED, OrderStatus.PAID, OrderStatus.RETURN_REQUESTED);

}