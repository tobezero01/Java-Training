package com.ducnhu.catalog.controller;

import com.ducnhu.catalog.dto.ProductDTO;
import com.ducnhu.catalog.service.ProductService;
import com.ducnhu.common.dto.PageResponse;
import com.ducnhu.common.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor // Tự động inject ProductService (Constructor Injection)
public class ProductRestController {

    private final ProductService productService;

    // 1. Get Detail by Alias
    @GetMapping("/alias/{alias}")
    public ResponseEntity<ProductDTO> byAlias(@PathVariable("alias") String alias) throws ProductNotFoundException {
        return ResponseEntity.ok(productService.getProduct(alias));
    }

    // 2. Get Detail by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> byId(@PathVariable("id") Integer id) throws ProductNotFoundException {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    // 3. List by Category (Có Page Size)
    @GetMapping("/by-category/{catId}")
    public ResponseEntity<PageResponse<ProductDTO>> byCategory(
            @PathVariable("catId") Integer catId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "name") String sort,
            @RequestParam(name = "dir", defaultValue = "asc") String dir) {

        // Truyền size xuống service
        return ResponseEntity.ok(productService.listByCategoryPaged(catId, page, size, sort, dir));
    }

    // 4. Search (Có Page Size)
    @GetMapping("/search")
    public ResponseEntity<PageResponse<ProductDTO>> search(
            @RequestParam(name = "keyword") String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        return ResponseEntity.ok(productService.search(keyword, page, size));
    }

    // 5. Featured / Home Page (Có Page Size)
    @GetMapping("/featured")
    public ResponseEntity<PageResponse<ProductDTO>> featured(
            @RequestParam(name = "type", defaultValue = "top-rated") String type,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) { // Thêm size

        // type: "top-rated", "most-reviewed", "new-arrival"
        return ResponseEntity.ok(productService.listFeaturedProducts(type, page, size));
    }
}