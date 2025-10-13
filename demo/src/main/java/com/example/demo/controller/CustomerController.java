package com.example.demo.controller;

import com.example.demo.entity.Customer;
import com.example.demo.obj.CustomerDTO;
import com.example.demo.projection.CustomerProjection;
import com.example.demo.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/customer")
public class CustomerController {
    @Autowired
    private CustomerService customerService;

    @GetMapping("/findByName")
    public List<Customer> findByName(@RequestParam (required = false) String name) {
        return customerService.findByName(name);
    }

    @GetMapping("/testDTO")
    public List<CustomerDTO> test() {
        return customerService.getCustomerAndPhone();
    }

    @GetMapping("/testProjection")
    public List<CustomerProjection> testProjection() {
        return customerService.getCustomerAndPhoneProjection();
    }
}
