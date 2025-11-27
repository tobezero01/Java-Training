package com.ducnhu.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // product snapshot
    private Integer productId;
    @Column(length = 512)
    private String productName;
    @Column(length = 256)
    private String productAlias;
    @Column(length = 256)
    private String productImage;

    private int quantity;
    private float productCost;
    private float shippingCost;
    private float unitPrice;
    private float subtotal;
}
