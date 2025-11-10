package com.ducnhu.cart.dto;

public record CartLineView(
        Integer productId,
        String name,
        String alias,
        String image,
        Integer quantity,
        Float price,
        Float discountPrice,
        Float subtotal
) {}
