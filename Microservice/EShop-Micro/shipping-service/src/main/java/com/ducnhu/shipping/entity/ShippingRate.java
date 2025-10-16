package com.ducnhu.shipping.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipping_rates",
        uniqueConstraints = @UniqueConstraint(name = "uk_ship_country_state", columnNames = {"country_id", "state"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_id", nullable = false)
    private Integer countryId;
    @Column(nullable = false)
    private String state;
    @Column(nullable = false)
    private Float rate;
    @Column(nullable = false)
    private Integer days;
    @Column(name = "cod_supported", nullable = false)
    private Boolean codSupported;
}
