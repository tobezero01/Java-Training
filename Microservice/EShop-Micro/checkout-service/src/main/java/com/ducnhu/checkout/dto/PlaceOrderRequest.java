package com.ducnhu.checkout.dto;

public record PlaceOrderRequest(Integer addressId, String paymentMethod, String note) {
}

