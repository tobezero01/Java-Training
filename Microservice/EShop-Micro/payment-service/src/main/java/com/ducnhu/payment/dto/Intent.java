package com.ducnhu.payment.dto;

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
}
