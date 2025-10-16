package com.ducnhu.common.events.orders;

public record OrderPlacedItem(
        Integer productId, String name, String alias, String image,
        Float unitPrice, Integer quantity, Float subtotal, Float shippingCost
){}
