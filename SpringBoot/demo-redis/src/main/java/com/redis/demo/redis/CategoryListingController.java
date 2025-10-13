package com.redis.demo.redis;

import com.redis.demo.redis.mapper.PageResult;
import com.redis.demo.redis.mapper.ProductDto;
import com.redis.demo.redis.mapper.ProductSummary;
import com.redis.demo.redis.service.ProductPageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CategoryListingController {

    private final ProductPageService pageService;

    public CategoryListingController(ProductPageService pageService) {
        this.pageService = pageService;
    }

    /** 1) Lấy product theo cateId (trang IDs -> summary) */
    @GetMapping("/categories/{catId}/products")
    public ResponseEntity<PageResult<ProductSummary>> getCategoryPage(
            @PathVariable int catId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created") String sort // hiện mới hỗ trợ 'created'
    ) {
        // (sort ở đây chưa dùng, bạn có thể mở rộng thêm ZSET khác theo sort)
        var result = pageService.getCategoryPage(catId, page, size);
        return ResponseEntity.ok(result);
    }

    /** 3) Lấy toàn bộ product theo "page ids" (client gửi IDs -> server trả DTO đầy đủ) */
    @PostMapping("/products/by-ids")
    public ResponseEntity<List<ProductDto>> getByIds(@RequestBody Map<String, List<Integer>> body) {
        var ids = body.getOrDefault("ids", List.of());
        var result = pageService.getByIds(ids);
        return ResponseEntity.ok(result);
    }
}
