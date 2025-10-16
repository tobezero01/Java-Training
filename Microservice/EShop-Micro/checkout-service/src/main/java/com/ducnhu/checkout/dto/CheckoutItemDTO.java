package com.ducnhu.checkout.dto;

public record CheckoutItemDTO(Integer productId, String name, String alias, String image, Float unitPrice,
                              Integer quantity, Float subtotal) {
}
