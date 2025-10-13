package com.ducnhu.geo.controller;

import com.ducnhu.geo.dto.CountryDTO;
import com.ducnhu.geo.dto.StateDTO;
import com.ducnhu.geo.entity.Country;
import com.ducnhu.geo.entity.State;
import com.ducnhu.geo.repository.CountryRepository;
import com.ducnhu.geo.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoRestController {

    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;

    @GetMapping("/countries")
    public List<CountryDTO> countries() {
        return countryRepository.findAllByOrderByNameAsc()
                .stream().map(c -> new CountryDTO(c.getId(), c.getName(), c.getCode()))
                .toList();
    }

    @GetMapping("/countries/{id}")
    public CountryDTO country(@PathVariable Integer id){
        Country c = countryRepository.findById(id).orElse(null);
        return (c==null)? null : new CountryDTO(c.getId(), c.getName(), c.getCode());
    }

    @GetMapping("/countries/{id}/states")
    public List<StateDTO> statesByCountry(@PathVariable Integer id) {
        List<State> states = stateRepository.findByCountryOrderByNameAsc(new Country(id));
        return states.stream().map(s -> new StateDTO(s.getId(), s.getName())).toList();
    }
}
