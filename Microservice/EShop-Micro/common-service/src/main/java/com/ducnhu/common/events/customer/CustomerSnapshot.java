package com.ducnhu.common.events.customer;

public record CustomerSnapshot(
        Integer id, String email, String firstName, String lastName, String phoneNumber
) {
}
