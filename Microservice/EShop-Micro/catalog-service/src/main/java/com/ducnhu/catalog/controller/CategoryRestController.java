package com.ducnhu.catalog.controller;

import com.ducnhu.catalog.dto.CategoryDTO;
import com.ducnhu.catalog.dto.CategoryNodeDTO;
import com.ducnhu.catalog.service.CategoryService;
import com.ducnhu.common.exception.CategoryNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryRestController {
    private final CategoryService categoryService;

    public CategoryRestController(CategoryService s) {
        this.categoryService = s;
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategoryNodeDTO>> tree() {
        return ResponseEntity.ok(categoryService.listCategoryTree());
    }

    @GetMapping("/top-level")
    public ResponseEntity<List<CategoryDTO>> top() {
        return ResponseEntity.ok(categoryService.listTopLevelParents());
    }

    @GetMapping("/leaf")
    public ResponseEntity<List<CategoryDTO>> leaves() {
        return ResponseEntity.ok(categoryService.listNoChildrenCategories());
    }

    @GetMapping("/alias/{alias}")
    public ResponseEntity<CategoryDTO> byAlias(@PathVariable("alias") String alias) throws CategoryNotFoundException {
        return ResponseEntity.ok(categoryService.getCategory(alias));
    }

    @GetMapping("/{id}/parents")
    public ResponseEntity<List<CategoryDTO>> parents(@PathVariable("id") Integer id) throws CategoryNotFoundException {
        return ResponseEntity.ok(categoryService.getAncestorsPath(id));
    }
}
