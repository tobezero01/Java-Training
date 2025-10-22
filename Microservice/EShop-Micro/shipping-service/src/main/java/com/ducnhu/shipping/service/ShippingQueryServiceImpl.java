package com.ducnhu.shipping.service;

import com.ducnhu.shipping.entity.ShippingRate;
import com.ducnhu.shipping.repository.ShippingRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingQueryServiceImpl implements ShippingQueryService{
    private final ShippingRateRepository repository;
    private String norm(String v) {
        return v == null ? "" : v.trim().toLowerCase();
    }
    @Override
    public ShippingRate get(Integer countryId, String stateOrCity) {
        String key =  norm(stateOrCity);
        return repository.findByCountryIdAndStateIgnoreCase(countryId, key).orElse(null);
    }
}
