package com.ducnhu.customer.client;

import com.ducnhu.customer.dto.CountryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "geo-service")
public interface GeoClient {
    @GetMapping("/api/geo/countries/{id}")
    CountryDTO country(@PathVariable("id") Integer id);

}
