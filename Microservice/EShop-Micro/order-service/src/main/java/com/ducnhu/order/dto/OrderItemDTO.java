package com.ducnhu.order.dto;

public record OrderItemDTO(
        Integer productId,
        String name,
        String alias,
        String image,
        Float unitPrice,
        Integer quantity,
        Float subtotal
) {}
