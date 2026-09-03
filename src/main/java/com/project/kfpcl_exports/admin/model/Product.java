package com.project.kfpcl_exports.admin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "admin_products")
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

    private String mainImageUrl;

    private Double rating;

    private Integer reviewCount;

    private Boolean trending;

    private Boolean active;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

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
