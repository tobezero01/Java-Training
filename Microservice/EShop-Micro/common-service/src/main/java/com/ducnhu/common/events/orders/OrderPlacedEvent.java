package com.ducnhu.common.events.orders;

import java.util.Date;
import java.util.List;

public record OrderPlacedEvent(
        String eventId, String orderNumber,
        Integer customerId, String customerEmail,
        Integer addressId, String addressLine,
        Float productTotal, Float shippingCost, Float paymentTotal,
        String paymentMethod, Date createdAt,
        List<OrderPlacedItem> items
) {}
