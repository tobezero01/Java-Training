package com.ducnhu.customer.mapper;

import com.ducnhu.customer.dto.AddressDTO;
import com.ducnhu.customer.dto.CreateAddressRequest;
import com.ducnhu.customer.dto.UpdateAddressRequest;
import com.ducnhu.customer.entity.Address;

public final class AddressMapper {
    private AddressMapper() {
    }

    public static AddressDTO toDto(Address a) {
        return new AddressDTO(
                a.getId(),
                a.getFirstName(),
                a.getLastName(),
                a.getPhoneNumber(),
                a.getAddressLine1(),
                a.getAddressLine2(),
                a.getCity(),
                a.getState(),
                a.getPostalCode(),
                a.getCountryId() != null ? a.getCountryId() : null,
                a.getCountryName() != null ? a.getCountryName() : null,
                a.isDefaultForShipping()
        );
    }

    public static Address fromCreate(CreateAddressRequest request) {
        Address address = new Address();
        address.setFirstName(request.firstName());
        address.setLastName(request.lastName());
        address.setPhoneNumber(request.phoneNumber());
        address.setAddressLine1(request.addressLine1());
        address.setAddressLine2(request.addressLine2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        if (request.countryId() != null) address.setCountryId(request.countryId());
        address.setDefaultForShipping(Boolean.TRUE.equals(request.defaultForShipping()));
        return address;
    }

    public static void applyUpdate(Address address, UpdateAddressRequest request) {
        if (request.firstName() != null) address.setFirstName(request.firstName());
        if (request.lastName() != null) address.setLastName(request.lastName());
        if (request.phoneNumber() != null) address.setPhoneNumber(request.phoneNumber());
        if (request.addressLine1() != null) address.setAddressLine1(request.addressLine1());
        if (request.addressLine2() != null) address.setAddressLine2(request.addressLine2());
        if (request.city() != null) address.setCity(request.city());
        if (request.state() != null) address.setState(request.state());
        if (request.postalCode() != null) address.setPostalCode(request.postalCode());
        if (request.countryId() != null) address.setCountryId(request.countryId());
        if (request.defaultForShipping() != null) address.setDefaultForShipping(request.defaultForShipping());
    }
}
