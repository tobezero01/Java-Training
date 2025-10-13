package com.redis.demo.redis.mapper;// src/main/java/com/eshop/util/MapperUtil.java


import com.redis.demo.redis.entity.Product;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public final class MapperUtil {
    private MapperUtil() {}

    public static ProductDto toDto(Product p) {
        ProductDto dto = new ProductDto();
        dto.id = p.getId();
        dto.name = p.getName();
        dto.alias = p.getAlias();
        dto.shortDescription = p.getShortDescription();
        dto.fullDescription = p.getFullDescription();
        dto.price = p.getPrice();
        dto.discountPercent = p.getDiscountPercent();
        dto.discountPrice = p.getDiscountPrice();
        dto.inStock = p.isInStock();
        dto.categoryId = (p.getCategory() != null ? p.getCategory().getId() : null);
        dto.brandId = (p.getBrand() != null ? p.getBrand().getId() : null);
        dto.mainImage = p.getMainImage();

        dto.images = p.getImages().stream()
                .map(img -> img.getName())
                .collect(Collectors.toList());

        // Giữ thứ tự thêm detail
        dto.details = new LinkedHashMap<>();
        p.getDetails().forEach(d -> dto.details.put(d.getName(), d.getValue()));

        return dto;
    }
}
