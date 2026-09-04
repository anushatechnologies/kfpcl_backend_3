package com.project.kfpcl_exports.buyer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.kfpcl_exports.admin.model.Product;
import com.project.kfpcl_exports.buyer.enums.RfqStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "BuyerRfq")
@Table(name = "buyer_rfqs", indexes = {
        @Index(name = "idx_rfq_code", columnList = "rfq_code", unique = true),
        @Index(name = "idx_rfq_buyer", columnList = "buyer_id"),
        @Index(name = "idx_rfq_product", columnList = "product_id"),
        @Index(name = "idx_rfq_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rfq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_code", nullable = false, unique = true, length = 64)
    private String rfqCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    @JsonIgnore
    private User buyer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String quantity;

    @Column(name = "delivery_location", nullable = false, length = 255)
    private String deliveryLocation;

    @Column(name = "buyer_message", length = 2000)
    private String buyerMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private RfqStatus status = RfqStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_rfq_id")
    @JsonIgnore
    private Rfq parentRfq;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("createdAt DESC")
    private List<RfqResponse> responses = new ArrayList<>();

    public RfqResponse getLatestResponse() {
        if (responses != null && !responses.isEmpty()) {
            return responses.get(0);
        }
        return null;
    }
}
