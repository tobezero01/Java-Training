package com.ducnhu.checkout.dto;

import java.util.List;

public record CheckoutSummaryDTO(List<CheckoutItemDTO> items, Float productTotal, Float shippingCost, Float paymentTotal,
                                 boolean shippingSupported, Integer addressId, String addressLine) {}
