package com.ducnhu.cart.dto;

import java.util.List;

public record CartGetResponse(
        List<CartLineView> items,
        int itemCount,
        int totalQuantity,
        float totalAmount
) {}