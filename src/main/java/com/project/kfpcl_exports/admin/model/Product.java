package com.project.kfpcl_exports.admin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
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
