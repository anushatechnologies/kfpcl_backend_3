package com.project.kfpcl_exports.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductRequestDTO {

    @JsonAlias({"name", "productName"})
    private String title;

    private String description;

    private Double price;

    private Double originalPrice;

    @JsonAlias({"stockQuantity", "stock"})
    private Integer stock;

    private String unit;

    private Long categoryId;

    private String categoryName;

    private Long subcategoryId;

    private String subcategoryName;

    @JsonAlias({"store_id", "storeId"})
    private Long storeId;

    @JsonAlias({"store", "storeName"})
    private String storeName;

    @JsonAlias({"imageUrl", "image", "mainImage", "productImage", "photo"})
    private String mainImageUrl;

    private Double rating;

    private Integer reviewCount;

    private Boolean trending;

    private Boolean active;
}
