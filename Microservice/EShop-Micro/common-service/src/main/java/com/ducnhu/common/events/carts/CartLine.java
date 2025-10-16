package com.ducnhu.common.events.carts;

public record CartLine(
        Integer productId,
        String name,
        String alias,
        String image,
        Float price,
        Float discountPrice,
        Float cost,
        Float length, Float width, Float height, Float weight,
        Integer quantity
) {
}
