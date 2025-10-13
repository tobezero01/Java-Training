package com.example.demo.repository;

import com.example.demo.entity.Customer;
import com.example.demo.obj.CustomerDTO;
import com.example.demo.projection.CustomerProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Query("select c from Customer c where c.name = :name")
    List<Customer> findByName(String name);

    @Query(value = "select * from customer where name = 1?", nativeQuery = true)
    List<Customer> findByNameNative(String name);

    @Query("select new com.example.demo.obj.CustomerDTO (c.name, p.phoneNumber) from Customer c INNER JOIN Phone p ON c.id = p.customer.id")
    List<CustomerDTO> getCustomerAndPhone();

    @Query("select c.name as name , p.phoneNumber as phoneNumber from Customer c INNER JOIN Phone p ON c.id = p.customer.id")
    List<CustomerProjection> getCustomerAndPhoneProjection();
}
