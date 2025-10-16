package com.ducnhu.common.events.shipping;

public record ShippingRateRequest(
        String correlationId, String replyTo,
        Integer countryId, String stateOrCity
) {
}
