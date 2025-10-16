package com.ducnhu.common.events.carts;

public record CartGetRequest(String correlationId, String replyTo, Integer customerId) {
}

