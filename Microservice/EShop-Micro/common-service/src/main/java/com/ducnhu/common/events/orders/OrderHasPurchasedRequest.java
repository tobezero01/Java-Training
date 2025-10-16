package com.ducnhu.common.events.orders;

public record OrderHasPurchasedRequest(String correlationId, String replyTo,
                                       Integer customerId, Integer productId) {
}

