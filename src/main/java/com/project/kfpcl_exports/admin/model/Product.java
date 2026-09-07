package com.project.kfpcl_exports.admin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "AdminProduct")
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private Double price;

    private Double originalPrice;

    @com.fasterxml.jackson.annotation.JsonAlias({"stockQuantity", "stock"})
    private Integer stock;

    private String unit; // e.g. kg, metric ton, box

    private Long categoryId;

    private String categoryName;

    private Long subcategoryId;

    private String subcategoryName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "store_name")
    private String storeName;

    @Transient
    private Long storeId;

    public Long getStoreId() {
        if (store != null) {
            return store.getId();
        }
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
        if (this.store != null && this.store.getId() != null && !this.store.getId().equals(storeId)) {
            this.store = null;
        }
    }

    public String getStoreName() {
        if (store != null && store.getName() != null) {
            return store.getName();
        }
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void setStore(Store store) {
        this.store = store;
        if (store != null) {
            this.storeId = store.getId();
            this.storeName = store.getName();
        } else {
            this.storeId = null;
            this.storeName = null;
        }
    }

    @com.fasterxml.jackson.annotation.JsonSetter("store")
    public void setStoreFromJson(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) {
            this.store = null;
            this.storeId = null;
            this.storeName = null;
        } else if (node.isTextual()) {
            this.storeName = node.asText();
        } else if (node.isObject()) {
            Store s = new Store();
            if (node.has("id")) s.setId(node.get("id").asLong());
            if (node.has("name")) s.setName(node.get("name").asText());
            this.setStore(s);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAlias({"imageUrl", "image", "mainImage", "productImage", "photo"})
    private String mainImageUrl;

    @com.fasterxml.jackson.annotation.JsonProperty("imageUrl")
    public String getImageUrl() {
        return mainImageUrl;
    }

    public void setImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            this.mainImageUrl = imageUrl;
        }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("image")
    public String getImage() {
        return mainImageUrl;
    }

    public void setImage(String image) {
        if (image != null && !image.isEmpty()) {
            this.mainImageUrl = image;
        }
    }

    private Double rating;

    private Integer reviewCount;

    private Boolean trending;

    private Boolean active;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (stock == null) {
            stock = 0;
        }
        if (rating == null) {
            rating = 5.0;
        }
        if (reviewCount == null) {
            reviewCount = 0;
        }
        if (trending == null) {
            trending = false;
        }
        if (active == null) {
            active = true;
        }
    }
}
