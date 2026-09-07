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

    @Column(name = "name")
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

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "store_name")
    private String storeName;

    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("store")
    public String getStore() {
        return storeName;
    }

    @Transient
    public void setStore(String store) {
        if (store != null && !store.isEmpty()) {
            this.storeName = store;
        }
    }

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

    @Column(name = "original_price")
    private Double originalPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "unit", length = 50)
    private String unit;

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

    @Transient
    public String getTitle() {
        return name;
    }

    @Transient
    public void setTitle(String title) {
        this.name = title;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        this.isActive = active;
    }

    @Transient
    public String getImageUrl() {
        return imageUrl != null ? imageUrl : mainImageUrl;
    }

    @Transient
    public String getMainImageUrl() {
        return mainImageUrl != null ? mainImageUrl : imageUrl;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("price")
    public Double getPrice() {
        if (numericPrice != null) {
            return numericPrice.doubleValue();
        }
        if (indicativePrice != null) {
            try {
                return Double.parseDouble(indicativePrice.replaceAll("[^0-9.]", ""));
            } catch (Exception ignored) {}
        }
        if (originalPrice != null) {
            return originalPrice;
        }
        return 0.0;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("mrp")
    public Double getMrp() {
        if (originalPrice != null && originalPrice > 0) {
            return originalPrice;
        }
        Double p = getPrice();
        return (p != null && p > 0) ? p : 0.0;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("variants")
    public java.util.List<java.util.Map<String, Object>> getVariants() {
        java.util.List<java.util.Map<String, Object>> vars = new java.util.ArrayList<>();
        Double p = getPrice();
        Double m = getMrp();
        int stock = (stockQuantity != null && stockQuantity > 0) ? stockQuantity : 100;

        java.util.Map<String, Object> standardVariant = new java.util.LinkedHashMap<>();
        standardVariant.put("id", id != null ? id : 1L);
        standardVariant.put("name", (name != null && !name.isBlank()) ? name + " (Standard pack)" : "Standard pack");
        standardVariant.put("price", p != null ? p : 0.0);
        standardVariant.put("discountPrice", (m != null && p != null && m > p) ? p : null);
        standardVariant.put("mrp", m != null ? m : p);
        standardVariant.put("stockQuantity", stock);
        standardVariant.put("isActive", isActive != null ? isActive : true);
        vars.add(standardVariant);
        return vars;
    }
}

