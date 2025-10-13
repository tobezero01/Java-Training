package com.ducnhu.customer.dto;

public record CreateAddressRequest(
        String firstName, String lastName, String phoneNumber,
        String addressLine1, String addressLine2, String city, String state, String postalCode,
        Integer countryId, Boolean defaultForShipping
) {
}
