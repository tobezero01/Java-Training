package com.ducnhu.auth.security;

import com.ducnhu.auth.entity.Customer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomerUserDetails implements UserDetails {
    private final Customer customer;
    public CustomerUserDetails(Customer c){ this.customer = c; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities(){ return List.of(); }
    @Override public String getPassword(){ return customer.getPassword(); }
    @Override public String getUsername(){ return customer.getEmail(); }
    @Override public boolean isAccountNonExpired(){ return true; }
    @Override public boolean isAccountNonLocked(){ return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled(){ return customer.isEnabled(); }
    public String getFullName(){ return customer.getFullName(); }
    public Customer getCustomer(){ return customer; }
}

