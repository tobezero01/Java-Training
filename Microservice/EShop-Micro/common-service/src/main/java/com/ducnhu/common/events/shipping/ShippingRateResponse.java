package com.ducnhu.common.events.shipping;

public record ShippingRateResponse(
        String correlationId,
        Integer countryId, String stateOrCity,
        Float rate, Integer days, Boolean codSupported
) {}
