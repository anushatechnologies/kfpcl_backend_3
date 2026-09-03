package com.project.Anusha.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rfq_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfqResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    @JsonIgnore
    private Rfq rfq;

    @Column(name = "quoted_price")
    private Double quotedPrice;

    @Column(name = "available_quantity", length = 100)
    private String availableQuantity;

    @Column(name = "delivery_time", length = 100)
    private String deliveryTime;

    @Column(name = "response_message", length = 2000)
    private String responseMessage;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
