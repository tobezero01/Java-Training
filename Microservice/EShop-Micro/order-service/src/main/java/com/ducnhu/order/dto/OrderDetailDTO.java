package com.ducnhu.order.dto;

import java.util.Date;
import java.util.List;

public record OrderDetailDTO(
        String orderNumber,
        Date orderTime,
        String status,
        String paymentMethod,
        Float productTotal,
        Float shippingCost,
        Float paymentTotal,
        String shippingAddress,
        List<OrderItemDTO> items
) {}
