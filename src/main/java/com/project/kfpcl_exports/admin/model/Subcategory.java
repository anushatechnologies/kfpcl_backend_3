package com.project.kfpcl_exports.admin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "AdminSubcategory")
@Table(name = "admin_subcategories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private Long categoryId;

    private String categoryName;

    @com.fasterxml.jackson.annotation.JsonAlias({"image", "icon", "subcategoryImage", "subcategory_image"})
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
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}
