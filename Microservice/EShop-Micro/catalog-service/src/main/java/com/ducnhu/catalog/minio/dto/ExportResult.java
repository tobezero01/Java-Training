package com.ducnhu.catalog.minio.dto;

public record ExportResult(String objectName, String presignedUrl, long bytes) {
}

