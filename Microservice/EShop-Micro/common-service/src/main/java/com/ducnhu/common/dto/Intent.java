package com.ducnhu.common.dto;

import com.ducnhu.common.events.customer.AddressSnapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Intent implements Serializable {
    public String orderNumber;
    public String paypalOrderId;
    public float amount;
    public String currency;
    public Integer customerId;
    public String customerEmail;
    private AddressSnapshot shippingAddress;
    private Integer countryId;
    private Integer addressId;
}

