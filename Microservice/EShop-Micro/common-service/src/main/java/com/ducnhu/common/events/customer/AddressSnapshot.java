package com.ducnhu.common.events.customer;

public record AddressSnapshot(
        String firstName, String lastName,
        String phoneNumber,
        String line1, String line2,
        String city, String state, String postalCode,
        String country
) {
}
