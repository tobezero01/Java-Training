package com.ducnhu.common.events.customer;

public record AddressQueryRequest(
        String correlationId, String replyTo,
        Integer customerId, Integer addressId // addressId null -> lấy default
){}