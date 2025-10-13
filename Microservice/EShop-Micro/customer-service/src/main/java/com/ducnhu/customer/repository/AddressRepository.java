package com.ducnhu.customer.repository;

import com.ducnhu.customer.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByCustomerId(Integer customerId);

    @Query("SELECT a FROM Address a WHERE a.id=:addrId AND a.customerId=:customerId")
    Address findByIdAndCustomer(@Param("addrId") Integer addressId, @Param("customerId") Integer customerId);

    @Modifying
    @Query("DELETE FROM Address a WHERE a.id=:addrId AND a.customerId=:customerId")
    void deleteByIdAndCustomer(@Param("addrId") Integer addressId, @Param("customerId") Integer customerId);

    @Modifying
    @Query("UPDATE Address a SET a.defaultForShipping=true WHERE a.id=:id")
    void setDefaultAddress(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE Address a SET a.defaultForShipping=false WHERE a.id<>:id AND a.customerId=:customerId")
    void setNonDefaultAddressForOthers(@Param("id") Integer defaultId, @Param("customerId") Integer customerId);

    @Query("SELECT a FROM Address a WHERE a.customerId=:customerId AND a.defaultForShipping=true")
    Address findDefaultByCustomer(@Param("customerId") Integer customerId);
}
