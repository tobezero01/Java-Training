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
    public ResponseEntity<ProductDTO> byAlias(@PathVariable String alias) throws ProductNotFoundException {
        return ResponseEntity.ok(productService.getProduct(alias));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> byId(@PathVariable Integer id) throws ProductNotFoundException {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping("/by-category/{catId}")
    public PageResponse<ProductDTO> byCategory(@PathVariable Integer catId, @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "name") String sort,
                                               @RequestParam(defaultValue = "asc") String dir) {
        return productService.listByCategoryPaged(catId, page, sort, dir);
    }

    @GetMapping("/search")
    public PageResponse<ProductDTO> search(@RequestParam String keyword, @RequestParam(defaultValue = "1") int page) {
        return productService.search(keyword, page);
    }
}
