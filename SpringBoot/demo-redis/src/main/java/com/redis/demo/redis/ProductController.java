package com.redis.demo.redis;

import com.redis.demo.redis.mapper.ProductDto;
import com.redis.demo.redis.service.ProductService;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products") // prefix chung
public class ProductController {
    private final ProductService productService;
    private final StringRedisTemplate redisTemplate;

    public ProductController(ProductService productService,
                             StringRedisTemplate redisTemplate) {
        this.productService = productService;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable int id) {
        ProductDto dto = productService.getProductById(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<?> getRelated(@PathVariable int id) {
        List<Integer> ids = productService.getRelatedProductIds(id);
        return ResponseEntity.ok(ids);
    }

    // Đẩy product vào hàng đợi để worker xử lý (rebuild cache)
    @PostMapping("/{id}/enqueue")
    public ResponseEntity<?> enqueue(@PathVariable int id) {
        redisTemplate.opsForList().leftPush("queue:product", String.valueOf(id));
        return ResponseEntity.accepted().body(Map.of("queued", id));
    }
}
