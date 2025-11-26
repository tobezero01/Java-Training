package com.ducnhu.catalog.minio.dto;

// Một record mô tả "hàng" chuẩn hóa, writer sẽ ghi ra Excel
public record Row(Integer id, String name, String alias, Float price, Float discount,
                  Boolean inStock, Float avgRating, Integer reviewCount,
                  String categoryName, String createdDate) {

}
