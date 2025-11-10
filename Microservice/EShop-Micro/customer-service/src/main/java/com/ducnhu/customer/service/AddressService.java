package com.ducnhu.customer.service;

import com.ducnhu.customer.dto.AddressDTO;
import com.ducnhu.customer.entity.Address;

import java.util.List;

public interface AddressService {
    List<AddressDTO> listAddressBook(Integer customerId);

    void save(Address address);

    Address get(Integer addressId, Integer customerId);

    void delete(Integer addressId, Integer customerId);

    void setDefaultAddress(Integer defaultAddressId, Integer customerId);

    Address getDefaultAddress(Integer customerId);

}
