package com.ducnhu.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_number", length = 64, nullable = false, unique = true)
    private String orderNumber;
    @Column(nullable = false)
    private Integer customerId;
    @Column(length = 128)
    private String customerEmail;

    // address snapshot
    @Column(nullable = false, length = 45)
    private String firstName;
    @Column(nullable = false, length = 45)
    private String lastName;
    @Column(nullable = false, length = 15)
    private String phoneNumber;
    @Column(nullable = false, length = 64)
    private String addressLine1;
    @Column(length = 64)
    private String addressLine2;
    @Column(nullable = false, length = 45)
    private String city;
    @Column(nullable = false, length = 45)
    private String state;
    @Column(nullable = false, length = 10)
    private String postalCode;
    @Column(nullable = false, length = 45)
    private String country;

    private Date orderTime;
    private float shippingCost;
    private float productCost;
    private float subtotal;
    private float tax;
    private float total;
    private int deliverDays;
    private Date deliverDate;
    @Column(length = 256)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private OrderStatus status;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    // paid info
    @Column(length = 64)
    private String paymentTransactionId;
    private Date paidTime;
    private Float paidAmount;
    @Column(length = 12)
    private String paidCurrency;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("updatedTime ASC")
    private List<OrderTrack> orderTracks = new ArrayList<>();
}
