package com.ducnhu.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_cart_customer_product", columnNames = {"customer_id", "product_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(nullable = false)
    private Integer quantity;

    // snapshot để checkout/ship không phải gọi catalog theo từng item
    private String name;
    private String alias;
    private String image;

    private Float price;
    @Column(name = "discount_price")
    private Float discountPrice;
    private Float cost;

    private Float length;
    private Float width;
    private Float height;
    private Float weight;

    public Float getUnitPrice() {
        return (discountPrice != null && discountPrice > 0) ? discountPrice : price;
    }

    public Float getSubtotal() {
        Float u = getUnitPrice();
        return (u == null ? 0f : u) * (quantity == null ? 0 : quantity);
    }
}