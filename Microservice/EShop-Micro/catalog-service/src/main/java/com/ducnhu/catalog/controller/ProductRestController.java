package com.ducnhu.catalog.controller;

import com.ducnhu.catalog.dto.ProductDTO;
import com.ducnhu.catalog.service.ProductService;
import com.ducnhu.common.dto.PageResponse;
import com.ducnhu.common.exception.ProductNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {
    private final ProductService productService;

    public ProductRestController(ProductService s) {
        this.productService = s;
    }

    @GetMapping("/alias/{alias}")
    public ResponseEntity<ProductDTO> byAlias(@PathVariable("alias") String alias) throws ProductNotFoundException {
        return ResponseEntity.ok(productService.getProduct(alias));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> byId(@PathVariable("id") Integer id) throws ProductNotFoundException {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping("/by-category/{catId}")
    public PageResponse<ProductDTO> byCategory(@PathVariable("catId") Integer catId, @RequestParam(name = "page", defaultValue = "1") int page,
                                               @RequestParam(name = "sort", defaultValue = "name") String sort,
                                               @RequestParam(name = "dir", defaultValue = "asc") String dir) {
        return productService.listByCategoryPaged(catId, page, sort, dir);
    }

    @GetMapping("/search")
    public PageResponse<ProductDTO> search(@RequestParam(name = "keyword") String keyword,
                                           @RequestParam(name = "page", defaultValue = "1") int page) {
        return productService.search(keyword, page);
    }
}
