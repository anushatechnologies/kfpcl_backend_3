package com.project.kfpcl_exports.admin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "AdminCategory")
@Table(name = "admin_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @com.fasterxml.jackson.annotation.JsonAlias({"image", "icon", "categoryImage", "category_image"})
    private String imageUrl;

    @com.fasterxml.jackson.annotation.JsonProperty("image")
    public String getImage() {
        return imageUrl;
    }

    public void setImage(String image) {
        if (image != null && !image.isEmpty()) {
            this.imageUrl = image;
        }
    }

    private Double discount; // e.g. 10.0 for 10% discount

    @com.fasterxml.jackson.annotation.JsonAlias({"order", "sortOrder", "sort_order", "display_order"})
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @com.fasterxml.jackson.annotation.JsonProperty("displayOrder")
    public Integer getDisplayOrder() {
        return displayOrder != null ? displayOrder : 0;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("order")
    public Integer getOrder() {
        return displayOrder != null ? displayOrder : 0;
    }

    public void setOrder(Integer order) {
        if (order != null) {
            this.displayOrder = order;
        }
    }

    private Boolean active;

    private Boolean deleted;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (deleted == null) {
            deleted = false;
        }
        if (discount == null) {
            discount = 0.0;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}
