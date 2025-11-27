package com.ducnhu.order.dto;

import java.util.Date;

public record OrderSummaryDTO(
        String orderNumber,
        Date orderTime,
        String status,
        String paymentMethod,
        Integer totalItems,
        Float productTotal,
        Float shippingCost,
        Float paymentTotal
) {}