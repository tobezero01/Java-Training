package com.example.demo.service.impl;

import com.example.demo.entity.Customer;
import com.example.demo.obj.CustomerDTO;
import com.example.demo.projection.CustomerProjection;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerServiceImpl implements CustomerService {
    @Autowired
    private CustomerRepository repository;
    @Override
    public List<Customer> findByName(String name) {
        return repository.findByName(name);
    }

    @Override
    public List<CustomerDTO> getCustomerAndPhone() {
        List<CustomerDTO> list = repository.getCustomerAndPhone();
        List<CustomerProjection> projections = repository.getCustomerAndPhoneProjection();
        return repository.getCustomerAndPhone();
    }

    @Override
    public List<CustomerProjection> getCustomerAndPhoneProjection() {
        return repository.getCustomerAndPhoneProjection();
    }
}
