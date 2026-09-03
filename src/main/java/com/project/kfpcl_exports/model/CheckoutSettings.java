package com.project.kfpcl_exports.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checkout_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double minimumOrderAmount;

    private Double taxPercentage;

    private Double shippingFee;

    private Boolean enableCod;

    private Boolean enableOnlinePayment;

    private String currency;
}
