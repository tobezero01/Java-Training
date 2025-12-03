package com.ducnhu.catalog.mapper;


import com.ducnhu.catalog.dto.ProductDTO;
import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.entity.product.ProductImage;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ProductMapper {
    private ProductMapper() {
    }

    public static ProductDTO toDto(Product p) {
        Set<ProductImage> imgs = p.getImages();
        List<String> extraPaths =
                (imgs == null || imgs.isEmpty())
                        ? Collections.emptyList()
                        : imgs.stream()
                        .sorted(Comparator.comparing(ProductImage::getId)) // sắp xếp ổn định
                        .map(ProductImage::getImagePath)                   // dùng helper vừa viết
                        .toList();

        return new ProductDTO(
                p.getId(),
                p.getName(),
                p.getAlias(),
                p.getShortDescription(),
                p.getFullDescription(),
                p.getPrice(),
                p.getDiscountPrice(),
                p.getMainImagePath(),
                p.getAverageRating(),
                p.getReviewCount(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.isInStock(),
                extraPaths
        );
    }
}
