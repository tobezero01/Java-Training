package com.ducnhu.checkout.dto;

public record PlaceOrderResponse(boolean success, String orderNumber, Float productTotal, Float shippingCost,
                                 Float paymentTotal) {
}

