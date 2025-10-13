package com.ducnhu.customer.service;

import com.ducnhu.common.cache.CacheKey;
import com.ducnhu.common.cache.CacheTtl;
import com.ducnhu.common.cache.RedisCacheService;
import com.ducnhu.customer.client.GeoClient;
import com.ducnhu.customer.dto.AddressDTO;
import com.ducnhu.customer.dto.CountryDTO;
import com.ducnhu.customer.entity.Address;
import com.ducnhu.customer.mapper.AddressMapper;
import com.ducnhu.customer.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@Transactional
public class AddressServiceImpl implements AddressService {
    private final AddressRepository repo;
    private final RedisCacheService cache;
    private final GeoClient geo;

    public AddressServiceImpl(AddressRepository r, RedisCacheService c, GeoClient g) {
        this.repo = r;
        this.cache = c;
        this.geo = g;
    }

    @Override
    public List<AddressDTO> listAddressBook(Integer customerId) {
        return repo.findByCustomerId(customerId).stream().map(AddressMapper::toDto).toList();
    }

    @Override
    public void save(Address a) {
        // [STEP] Nếu có countryId → gọi geo-service lấy tên country (snapshot)
        if (a.getCountryId() != null) {
            CountryDTO c = geo.country(a.getCountryId());
            if (c == null) throw new IllegalArgumentException("Invalid country id: " + a.getCountryId());
            a.setCountryName(c.name());
        }
        repo.save(a);
    }

    @Override
    public Address get(Integer addressId, Integer customerId) {
        String key = CacheKey.addrById(customerId, addressId);
        return cache.getOrLoad(key, Address.class, CacheTtl.ADDRESS,
                () -> repo.findByIdAndCustomer(addressId, customerId));
    }

    @Override
    public void delete(Integer addressId, Integer customerId) {
        repo.deleteByIdAndCustomer(addressId, customerId);
    }

    @Override
    public void setDefaultAddress(Integer defaultId, Integer customerId) {
        if (defaultId != null && defaultId > 0) repo.setDefaultAddress(defaultId);
        repo.setNonDefaultAddressForOthers(defaultId, customerId);
    }

    @Override
    public Address getDefaultAddress(Integer customerId) {
        String key = CacheKey.addrDefault(customerId);
        return cache.getOrLoad(key, Address.class, Duration.ofMinutes(5),
                () -> repo.findDefaultByCustomer(customerId));
    }
}
