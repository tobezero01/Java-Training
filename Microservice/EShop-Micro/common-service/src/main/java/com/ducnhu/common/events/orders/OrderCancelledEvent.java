package com.ducnhu.common.events.orders;

import java.util.Date;

public record OrderCancelledEvent(
        String eventId,
        String orderNumber,
        Integer customerId,
        String reason,
        Date cancelledAt
) {}
