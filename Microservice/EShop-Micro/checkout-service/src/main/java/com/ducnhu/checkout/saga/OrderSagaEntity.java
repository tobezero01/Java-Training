package com.ducnhu.checkout.saga;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="order_sagas", indexes = @Index(name="idx_saga_status", columnList="status,updated_at"))
@Getter
@Setter
public class OrderSagaEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name="order_number", length=64, unique=true, nullable=false)
    private String orderNumber;

    @Column(name="customer_id", nullable=false)
    private Integer customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private Status status;                 // NEW/PUBLISHED/COMPLETED/CANCELLED

    @Column(name="note", length=512)
    private String note;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt = Instant.now();

    public enum Status { NEW, PUBLISHED, COMPLETED, CANCELLED }

}
