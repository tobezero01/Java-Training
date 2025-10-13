package com.example.demo.service;

import com.example.demo.entity.Customer;
import com.example.demo.obj.CustomerDTO;
import com.example.demo.projection.CustomerProjection;

import java.util.List;

public interface CustomerService{
    List<Customer> findByName(String name);
    List<CustomerDTO> getCustomerAndPhone();
    List<CustomerProjection> getCustomerAndPhoneProjection();
}
