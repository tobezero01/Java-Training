package com.ducnhu.payment.saga;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="payments", uniqueConstraints = @UniqueConstraint(name="uk_pay_order", columnNames="order_number"))
@Getter
@Setter
public class Payment {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="order_number", length=64, nullable=false)
    private String orderNumber;

    @Column(name="customer_id")  private Integer customerId;
    @Column(name="customer_email", length=128) private String customerEmail;

    @Column(name="paypal_order_id", length=64)   private String paypalOrderId;
    @Column(name="paypal_capture_id", length=64) private String paypalCaptureId;

    @Column(name="status", length=32)            private String status; // APPROVED/COMPLETED/FAILED...
    @Column(name="amount")                       private Float amount;
    @Column(name="currency", length=12)          private String currency;

    @Column(name="created_at")  private Instant createdAt = Instant.now();
    @Column(name="updated_at")  private Instant updatedAt = Instant.now();

}