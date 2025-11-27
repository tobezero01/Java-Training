package com.ducnhu.payment.dto;

import com.ducnhu.common.events.customer.AddressSnapshot;
import com.ducnhu.common.events.orders.OrderPlacedItem;

import java.util.List;

public record CheckoutSnapshot(
        Summary summary,
        AddressSnapshot shippingAddress,
        Integer countryId,
        List<OrderPlacedItem> items
) {}