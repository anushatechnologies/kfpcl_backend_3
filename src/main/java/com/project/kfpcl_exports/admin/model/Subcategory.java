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
    }
}
