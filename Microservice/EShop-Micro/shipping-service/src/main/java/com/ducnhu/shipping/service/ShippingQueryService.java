package com.ducnhu.shipping.service;

import com.ducnhu.shipping.entity.ShippingRate;

public interface ShippingQueryService {
    ShippingRate get(Integer countryId, String stateOrCity);
}
