package com.ducnhu.common.events.orders;


import com.ducnhu.common.events.customer.AddressSnapshot;

import java.util.Date;
import java.util.List;

public record OrderPaidEventV2(
        String correlationId,
        String orderNumber,
        Integer customerId,
        String customerEmail,
        String transactionId,
        Float paidAmount,
        String currency,
        Date paidTime,
        AddressSnapshot shippingAddress, // ⬅️ thêm snapshot để chống NOT NULL
        String paymentMethod,             // "PAYPAL"
        Integer deliverDays,        // số ngày dự kiến
        Date deliverDate,
        // NEW – snapshot chi tiết đơn tại thời điểm thanh toán
        Float productTotal,
        Float shippingCost,
        List<OrderPlacedItem> items
) {}
