package com.project.kfpcl_exports.admin.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonAlias({"image", "url"})
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

    private Boolean isPrimary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;
}
