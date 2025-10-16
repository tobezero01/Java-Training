package com.ducnhu.common.events.catalog;

public record ProductSnapshot(
        Integer id, String name, String alias, String mainImagePath,
        Float price, Float discountPrice, Float cost,
        Float length, Float width, Float height, Float weight,
        Boolean inStock
) {
}
