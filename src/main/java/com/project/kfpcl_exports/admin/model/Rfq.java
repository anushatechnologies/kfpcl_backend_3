package com.project.kfpcl_exports.admin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_rfqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rfq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String rfqNumber;

    private Long customerId;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private String productName;

    private Integer quantity;

    private String unit;

    private String targetPrice;

    private String shippingTerms;

    private String destinationCountry;

    @Column(length = 2000)
    private String details;

    private String status; // PENDING, QUOTED, REJECTED, ACCEPTED

    @OneToOne(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private Quotation quotation;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
