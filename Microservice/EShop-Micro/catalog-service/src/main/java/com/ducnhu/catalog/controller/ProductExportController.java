package com.ducnhu.catalog.controller;

import com.ducnhu.catalog.minio.ProductExcelExportService;
import com.ducnhu.catalog.minio.dto.ExportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductExportController {
    private final ProductExcelExportService service;

    @GetMapping("/export-excel")
    public ResponseEntity<ExportResult> export() {
        ExportResult r = service.exportAllProductsToExcel();
        return ResponseEntity.ok(r);
    }
}
