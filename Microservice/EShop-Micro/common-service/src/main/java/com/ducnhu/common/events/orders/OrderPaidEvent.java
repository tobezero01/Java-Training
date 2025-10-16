package com.ducnhu.common.events.orders;

import java.util.Date;

public record OrderPaidEvent(
        String eventId, String orderNumber,
        Integer customerId, String customerEmail,
        String transactionId, Float paidAmount, String currency, Date paidTime
) {
}
