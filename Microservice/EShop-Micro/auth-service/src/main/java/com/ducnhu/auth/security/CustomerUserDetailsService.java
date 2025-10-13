package com.ducnhu.auth.security;

import com.ducnhu.auth.entity.Customer;
import com.ducnhu.auth.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {
    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer c = customerRepository.findByEmail(email);
        if (c == null) throw new UsernameNotFoundException("No Customer with email: " + email);
        return new CustomerUserDetails(c);
    }

    @Transactional(readOnly = true)
    public CustomerUserDetails loadUserById(Integer id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found by id: " + id));
        return new CustomerUserDetails(c);
    }
}
