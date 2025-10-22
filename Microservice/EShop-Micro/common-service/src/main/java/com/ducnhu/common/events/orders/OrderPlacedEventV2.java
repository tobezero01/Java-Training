package com.ducnhu.common.events.orders;

import com.ducnhu.common.events.customer.AddressSnapshot;
import com.ducnhu.common.events.customer.CustomerSnapshot;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public record OrderPlacedEventV2(
        String eventId,
        String orderNumber,
        Date createdAt,
        CustomerSnapshot customer,
        AddressSnapshot shippingAddress,
        List<OrderPlacedItem> items,
        Float productTotal,
        Float shippingCost,
        Float paymentTotal,
        String paymentMethod
) {}
