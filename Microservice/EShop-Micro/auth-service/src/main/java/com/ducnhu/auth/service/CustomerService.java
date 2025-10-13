package com.ducnhu.auth.service;

import com.ducnhu.auth.dto.ProfileResponse;
import com.ducnhu.auth.dto.ProfileUpdateRequest;
import com.ducnhu.auth.entity.AuthenticationType;
import com.ducnhu.auth.entity.Customer;

public interface CustomerService {
    Customer getCustomerByEmail(String email);
    ProfileResponse getCurrentProfile();
    ProfileResponse updateCurrentProfile(ProfileUpdateRequest req);
    void updateAuthenticationType(Customer customer, AuthenticationType type);
    void addCustomerUponOAuthLogin(String name, String email, String countryCode);
}
