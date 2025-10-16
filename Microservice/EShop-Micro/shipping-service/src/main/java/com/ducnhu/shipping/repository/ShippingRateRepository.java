package com.ducnhu.shipping.repository;

import com.ducnhu.shipping.entity.ShippingRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {
    Optional<ShippingRate> findByCountryIdAndState(Integer countryId, String state);
}
