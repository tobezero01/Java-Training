package com.ducnhu.checkout.saga;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class OrderSagaState implements Serializable {
    public enum Status { NEW, PUBLISHED, CANCELLED, COMPLETED }
    private String orderNumber;
    private Integer customerId;
    private Status status;
    private String note;
    private Date updatedAt;

}
