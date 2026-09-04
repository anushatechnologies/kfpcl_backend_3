package com.project.kfpcl_exports.buyer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "BuyerProduct")
@Table(name = "buyer_products")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    @com.fasterxml.jackson.annotation.JsonAlias({"title", "name", "productName"})
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String brand;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "main_image_url", columnDefinition = "TEXT")
    private String mainImageUrl;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "product_gallery_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> galleryImages = new ArrayList<>();

    @Column(name = "min_order_quantity", length = 50)
    private String minOrderQuantity;

    @Column(name = "indicative_price", length = 50)
    private String indicativePrice;

    @Column(name = "numeric_price")
    private BigDecimal numericPrice;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (imageUrl == null && mainImageUrl != null) {
            imageUrl = mainImageUrl;
        } else if (mainImageUrl == null && imageUrl != null) {
            mainImageUrl = imageUrl;
        }
    }

    public String getTitle() {
        return name;
    }

    public void setTitle(String title) {
        this.name = title;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        this.isActive = active;
    }

    public String getImageUrl() {
        return imageUrl != null ? imageUrl : mainImageUrl;
    }

    public String getMainImageUrl() {
        return mainImageUrl != null ? mainImageUrl : imageUrl;
    }
}

